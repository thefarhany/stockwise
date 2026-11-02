package com.thefarhany.stockwise.service.report;

import com.thefarhany.stockwise.dto.insight.SummaryResponse;
import com.thefarhany.stockwise.dto.report.DailyReportResponse;
import com.thefarhany.stockwise.dto.report.RangeReportResponse;
import com.thefarhany.stockwise.dto.report.SessionListItem;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.insight.DailySummary;
import com.thefarhany.stockwise.model.sales.SaleStatus;
import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.SessionStatus;
import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.repository.insight.DailySummaryRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.repository.sales.SaleItemRepository;
import com.thefarhany.stockwise.repository.sales.SaleRepository;
import com.thefarhany.stockwise.repository.session.DailySessionRepository;
import com.thefarhany.stockwise.repository.session.SessionStockRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.insight.SummaryService;
import com.thefarhany.stockwise.service.session.SessionService;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import com.thefarhany.stockwise.utils.report.ReportRanges;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Laporan dan riwayat untuk Owner. Hanya membaca; angka sesi sudah final sejak sesi ditutup. */
@Service
public class ReportService {

	static final int DEFAULT_RANGE_DAYS = 31;

	private final SessionService sessionService;
	private final DailySessionRepository sessions;
	private final DailySummaryRepository summaries;
	private final SessionStockRepository stocks;
	private final SaleRepository sales;
	private final SaleItemRepository saleItems;
	private final OutletRepository outlets;
	private final Clock clock;

	public ReportService(SessionService sessionService, DailySessionRepository sessions,
			DailySummaryRepository summaries, SessionStockRepository stocks, SaleRepository sales,
			SaleItemRepository saleItems, OutletRepository outlets, Clock clock) {
		this.sessionService = sessionService;
		this.sessions = sessions;
		this.summaries = summaries;
		this.stocks = stocks;
		this.sales = sales;
		this.saleItems = saleItems;
		this.outlets = outlets;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DailyReportResponse daily(AuthPrincipal me, LocalDate date) {
		DailySession session = sessionService.findByDate(me.outletId(), date).filter(s -> s.getStatus() != SessionStatus.OPEN).orElseThrow(
				() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tidak ada sesi tertutup pada tanggal ini."));
		DailySummary s = summaries.findBySessionId(session.getId()).orElseThrow(
				() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ringkasan belum tersedia."));

		Map<Integer, BigDecimal> perHour = new TreeMap<>();
		for (SaleItemRepository.SoldAt row : saleItems.soldTimes(session.getId())) {
			int hour = row.getSoldAt().atZone(OutletRules.zone(session.getOutlet())).getHour();
			perHour.merge(hour, row.getQty(), BigDecimal::add);
		}
		Map<String, Long> perPayment = sales.paymentTotals(session.getId()).stream()
				.collect(Collectors.toMap(t -> t.getMethod().name(), SaleRepository.PaymentTotal::getTotal));

		return new DailyReportResponse(date, s.getRevenue(), s.getCogs(), s.getWasteCost(), s.getAdjustmentCost(),
				s.getCarriedValue(), s.getExpenseTotal(), s.getNetProfit(),
				sales.countBySessionIdAndStatus(session.getId(), SaleStatus.COMPLETED), s.getVoidCount(),
				saleItems.productSales(session.getId()).stream().map(p -> new DailyReportResponse.ProductLine(
						p.getProductId(), p.getName(), p.getQty().stripTrailingZeros(), p.getRevenue())).toList(),
				perHour.entrySet().stream()
						.map(e -> new DailyReportResponse.HourLine(e.getKey(), e.getValue().stripTrailingZeros()))
						.toList(),
				perPayment);
	}

	/** Tren per hari atau per minggu (Senin awal minggu ISO) dari sesi tertutup (FR-26). */
	@Transactional(readOnly = true)
	public RangeReportResponse range(AuthPrincipal me, LocalDate from, LocalDate to, String groupBy) {
		ReportRanges.requireValid(from, to);
		boolean weekly = switch (groupBy) {
			case "day" -> false;
			case "week" -> true;
			default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
					Map.of("fields", Map.of("groupBy", "harus day atau week")));
		};
		List<DailySummary> rows = summaries.findInRange(me.outletId(), from, to);
		Map<LocalDate, List<DailySummary>> byPeriod = rows.stream().collect(Collectors.groupingBy(
				s -> weekly ? s.getSession().getBusinessDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
						: s.getSession().getBusinessDate(),
				TreeMap::new, Collectors.toList()));
		List<RangeReportResponse.Point> points = byPeriod.entrySet().stream().map(e -> {
			RangeReportResponse.Totals t = totals(e.getValue());
			return new RangeReportResponse.Point(e.getKey(), t.revenue(), t.cogs(), t.wasteCost(), t.netProfit(),
					t.itemsSold(), t.sessions());
		}).toList();
		return new RangeReportResponse(from, to, groupBy, points, totals(rows));
	}

	private static RangeReportResponse.Totals totals(List<DailySummary> rows) {
		return new RangeReportResponse.Totals(rows.stream().mapToLong(DailySummary::getRevenue).sum(),
				rows.stream().mapToLong(DailySummary::getCogs).sum(),
				rows.stream().mapToLong(DailySummary::getWasteCost).sum(),
				rows.stream().mapToLong(DailySummary::getNetProfit).sum(),
				rows.stream().map(DailySummary::getTotalItemsSold).reduce(BigDecimal.ZERO, BigDecimal::add)
						.stripTrailingZeros(),
				rows.size());
	}

	/** Riwayat sesi, terbaru dulu. Tanpa rentang: 31 hari terakhir sampai hari ini (tanggal dagang usaha). */
	@Transactional(readOnly = true)
	public Page<SessionListItem> history(AuthPrincipal me, LocalDate from, LocalDate to, int page, int size) {
		LocalDate end = to != null ? to : OutletRules.today(outlets.findById(me.outletId()).orElseThrow(), clock);
		LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1L);
		if (start.isAfter(end)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
					Map.of("fields", Map.of("from", "tidak boleh setelah tanggal akhir")));
		}
		Page<DailySession> found = sessions.findByOutletIdAndBusinessDateBetween(me.outletId(), start, end,
				PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
						Sort.by(Sort.Direction.DESC, "businessDate")));
		List<UUID> ids = found.getContent().stream().map(DailySession::getId).toList();
		Map<UUID, DailySummary> summaryBySession = summaries.findBySessionIdIn(ids).stream()
				.collect(Collectors.toMap(x -> x.getSession().getId(), Function.identity()));
		Map<UUID, List<SessionStock>> primaryRows = stocks.findBySessionIdInAndStockItemPrimaryTrue(ids).stream()
				.collect(Collectors.groupingBy(r -> r.getSession().getId()));
		return found.map(session -> item(session, summaryBySession.get(session.getId()),
				primaryRows.getOrDefault(session.getId(), List.of())));
	}

	private static SessionListItem item(DailySession session, DailySummary s, List<SessionStock> primaryRows) {
		SessionListItem.PrimaryItem primary = null;
		if (s != null) {
			primary = SummaryService.primaryItem(s, primaryRows).map(p -> new SessionListItem.PrimaryItem(p.name(),
					p.soldQty(), p.soldOutAt(), recommendedFor(primaryRows, p))).orElse(null);
		}
		else {
			// sesi terbuka: belum ada ringkasan, tampilkan item utama dengan jumlah terjual sejauh ini
			primary = primaryRows.stream().max(java.util.Comparator.comparing(SessionStock::getSoldQty))
					.map(r -> new SessionListItem.PrimaryItem(r.getStockItem().getName(), r.getSoldQty(),
							r.getSoldOutAt(), r.getRecommendedQty())).orElse(null);
		}
		return new SessionListItem(session.getId(), session.getBusinessDate(), session.getStatus(),
				session.getCloseType(), s == null ? null : s.getRevenue(), s == null ? null : s.getCogs(),
				s == null ? null : s.getWasteCost(), s == null ? null : s.getNetProfit(), primary);
	}

	private static BigDecimal recommendedFor(List<SessionStock> rows, SummaryResponse.PrimaryItem p) {
		return rows.stream().filter(r -> r.getStockItem().getId().equals(p.stockItemId())).findFirst()
				.map(SessionStock::getRecommendedQty).orElse(null);
	}

}
