package com.thefarhany.stockwise.service.insight;

import com.thefarhany.stockwise.dto.insight.SummaryResponse;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.insight.DailySummary;
import com.thefarhany.stockwise.model.insight.RecommendationStatus;
import com.thefarhany.stockwise.model.sales.SaleStatus;
import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.SessionStatus;
import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.repository.insight.DailySummaryRepository;
import com.thefarhany.stockwise.repository.insight.RecommendationRepository;
import com.thefarhany.stockwise.repository.sales.SaleItemRepository;
import com.thefarhany.stockwise.repository.sales.SaleRepository;
import com.thefarhany.stockwise.repository.session.DailySessionRepository;
import com.thefarhany.stockwise.repository.session.SessionStockRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.utils.insight.LostSalesEstimator;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Angka hasil sesi: dibuat sekali saat sesi ditutup, dibaca Owner kapan saja. */
@Service
public class SummaryService {

	private final DailySummaryRepository summaries;
	private final DailySessionRepository sessions;
	private final SessionStockRepository stocks;
	private final SaleRepository sales;
	private final SaleItemRepository saleItems;
	private final RecommendationRepository recommendations;

	public SummaryService(DailySummaryRepository summaries, DailySessionRepository sessions,
			SessionStockRepository stocks, SaleRepository sales, SaleItemRepository saleItems,
			RecommendationRepository recommendations) {
		this.summaries = summaries;
		this.sessions = sessions;
		this.stocks = stocks;
		this.sales = sales;
		this.saleItems = saleItems;
		this.recommendations = recommendations;
	}

	/** Dipanggil di dalam transaksi tutup sesi; semua kolom biaya sesi sudah terisi. */
	@Transactional
	public void create(DailySession session) {
		List<SessionStock> rows = stocks.findBySessionIdOrderByStockItemName(session.getId());
		long revenue = sales.completedRevenue(session.getId());
		long cogs = rows.stream().mapToLong(r -> nz(r.getSoldCost())).sum();
		long waste = rows.stream().mapToLong(r -> nz(r.getDiscardedCost())).sum();
		long adjustment = rows.stream().mapToLong(r -> nz(r.getAdjustmentCost())).sum();
		long carried = rows.stream().mapToLong(r -> nz(r.getCarriedValue())).sum();
		// laba sebelum biaya operasional: pengeluaran (expense_total) baru ada di R2, jadi 0
		summaries.save(new DailySummary(session, revenue, cogs, waste, adjustment, carried,
				revenue - cogs - waste - adjustment,
				saleItems.completedPortions(session.getId()),
				(int) sales.countBySessionIdAndStatus(session.getId(), SaleStatus.VOID),
				lostSales(session, rows)));
	}

	@Transactional(readOnly = true)
	public SummaryResponse get(AuthPrincipal me, UUID sessionId) {
		DailySession session = sessions.findByIdAndOutletId(sessionId, me.outletId()).orElseThrow(
				() -> new BusinessException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Sesi tidak ditemukan."));
		if (session.getStatus() == SessionStatus.OPEN) {
			throw new BusinessException(HttpStatus.CONFLICT, "SESSION_NOT_CLOSED",
					"Ringkasan tersedia setelah sesi ditutup.");
		}
		DailySummary s = summaries.findBySessionId(sessionId).orElseThrow(
				() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ringkasan belum tersedia."));
		List<SessionStock> rows = stocks.findBySessionIdOrderByStockItemName(sessionId);
		LocalDate nextDay = session.getBusinessDate().plusDays(1);
		return new SummaryResponse(sessionId, session.getBusinessDate(), session.getCloseType().name(),
				s.getRevenue(), s.getCogs(), s.getWasteCost(), s.getAdjustmentCost(), s.getCarriedValue(),
				s.getExpenseTotal(), s.getNetProfit(), s.getTotalItemsSold().stripTrailingZeros(), s.getVoidCount(),
				primaryItem(s, rows).orElse(null),
				new SummaryResponse.InsightBlock(s.getInsightStatus().name(), s.getInsightText(),
						s.getInsightModel(), InsightService.attemptsLeft(s), s.getInsightGeneratedAt()),
				recommendations.existsByOutletIdAndTargetDateAndStatus(me.outletId(), nextDay,
						RecommendationStatus.ACTIVE) ? nextDay : null);
	}

	/** Item utama yang jadi sorotan: yang habis dengan perkiraan terlewat terbesar, atau yang paling banyak terjual. */
	public static Optional<SummaryResponse.PrimaryItem> primaryItem(DailySummary s, List<SessionStock> rows) {
		Map<String, Object> lost = s.getLostSalesEstimate();
		if (lost != null) {
			UUID itemId = UUID.fromString((String) lost.get("stock_item_id"));
			return rows.stream().filter(r -> r.getStockItem().getId().equals(itemId)).findFirst()
					.map(r -> new SummaryResponse.PrimaryItem(itemId, r.getStockItem().getName(), r.getSoldQty(),
							r.getSoldOutAt(), BigDecimal.valueOf(((Number) lost.get("qty")).doubleValue())
									.setScale(1, RoundingMode.HALF_UP)));
		}
		return rows.stream().filter(r -> r.getStockItem().isPrimary())
				.max(Comparator.comparing(SessionStock::getSoldQty))
				.map(r -> new SummaryResponse.PrimaryItem(r.getStockItem().getId(), r.getStockItem().getName(),
						r.getSoldQty(), null, null));
	}

	/** Hanya item utama yang habis; bila lebih dari satu, yang perkiraannya terbesar. null bila tidak ada yang habis. */
	private static Map<String, Object> lostSales(DailySession session, List<SessionStock> rows) {
		LocalTime open = session.getOutlet().getOpenTime();
		LocalTime close = session.getPlannedCloseTime();
		Map<String, Object> best = null;
		BigDecimal bestQty = BigDecimal.valueOf(-1);
		for (SessionStock r : rows) {
			if (!r.getStockItem().isPrimary() || r.getSoldOutAt() == null) {
				continue;
			}
			LocalTime soldOut = r.getSoldOutAt().atZone(OutletRules.zone(session.getOutlet())).toLocalTime();
			BigDecimal qty = LostSalesEstimator.estimate(r.getSoldQty(), open, close, soldOut);
			if (qty.compareTo(bestQty) > 0) {
				bestQty = qty;
				best = new HashMap<>();
				best.put("stock_item_id", r.getStockItem().getId().toString());
				best.put("qty", qty.doubleValue());
				best.put("sold_qty", r.getSoldQty().doubleValue());
			}
		}
		return best;
	}

	private static long nz(Long v) {
		return v == null ? 0 : v;
	}

}
