package com.thefarhany.stockwise.service.insight;

import com.thefarhany.stockwise.model.catalog.Product;
import com.thefarhany.stockwise.model.catalog.RecipeItem;
import com.thefarhany.stockwise.model.catalog.StockItem;
import com.thefarhany.stockwise.model.insight.DailySummary;
import com.thefarhany.stockwise.model.insight.Forecast;
import com.thefarhany.stockwise.model.insight.Recommendation;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.model.session.CloseType;
import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.SessionStatus;
import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.repository.catalog.ProductRepository;
import com.thefarhany.stockwise.repository.catalog.RecipeItemRepository;
import com.thefarhany.stockwise.repository.catalog.StockItemRepository;
import com.thefarhany.stockwise.repository.insight.DailySummaryRepository;
import com.thefarhany.stockwise.repository.insight.ForecastRepository;
import com.thefarhany.stockwise.repository.insight.RecommendationRepository;
import com.thefarhany.stockwise.repository.sales.SaleItemRepository;
import com.thefarhany.stockwise.repository.session.DailySessionRepository;
import com.thefarhany.stockwise.repository.session.SessionStockRepository;
import com.thefarhany.stockwise.service.session.SessionService;
import com.thefarhany.stockwise.utils.catalog.UnitConverter;
import com.thefarhany.stockwise.utils.insight.DemandForecaster;
import com.thefarhany.stockwise.utils.insight.InsightTexts;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prediksi permintaan dan saran stok untuk hari berikutnya, dibuat otomatis saat sesi ditutup.
 * Aturan hitungan prediksi ada di DemandForecaster; di sini data diambil dan hasilnya diterjemahkan lewat resep.
 */
@Service
public class ForecastService {

	private static final String MODEL_VERSION = "baseline-1.0";
	private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH.mm");

	private final DailySessionRepository sessions;
	private final SaleItemRepository saleItems;
	private final DailySummaryRepository summaries;
	private final RecipeItemRepository recipes;
	private final ProductRepository products;
	private final StockItemRepository stockItems;
	private final SessionStockRepository stocks;
	private final ForecastRepository forecasts;
	private final RecommendationRepository recommendations;
	private final SessionService sessionService;

	public ForecastService(DailySessionRepository sessions, SaleItemRepository saleItems,
			DailySummaryRepository summaries, RecipeItemRepository recipes, ProductRepository products,
			StockItemRepository stockItems, SessionStockRepository stocks, ForecastRepository forecasts,
			RecommendationRepository recommendations, SessionService sessionService) {
		this.sessions = sessions;
		this.saleItems = saleItems;
		this.summaries = summaries;
		this.recipes = recipes;
		this.products = products;
		this.stockItems = stockItems;
		this.stocks = stocks;
		this.forecasts = forecasts;
		this.recommendations = recommendations;
		this.sessionService = sessionService;
	}

	/** Dipanggil di dalam transaksi tutup sesi, sesudah ringkasan angka disimpan. */
	@Transactional
	public void generate(DailySession session) {
		Outlet outlet = session.getOutlet();
		UUID outletId = outlet.getId();
		LocalDate target = session.getBusinessDate().plusDays(1);

		List<DailySession> recent = sessions.findByOutletIdAndStatusOrderByBusinessDateDesc(outletId,
				SessionStatus.CLOSED, PageRequest.of(0, DemandForecaster.WINDOW));
		List<UUID> sessionIds = recent.stream().map(DailySession::getId).toList();
		Map<UUID, List<SaleItemRepository.ProductDemand>> demandBySession = saleItems.demandBySession(sessionIds)
				.stream().collect(Collectors.groupingBy(SaleItemRepository.ProductDemand::getSessionId));
		Map<UUID, DailySummary> summaryBySession = summaries.findBySessionIdIn(sessionIds).stream()
				.collect(Collectors.toMap(s -> s.getSession().getId(), s -> s));
		Set<UUID> productIds = demandBySession.values().stream().flatMap(List::stream)
				.map(SaleItemRepository.ProductDemand::getProductId).collect(Collectors.toSet());
		Map<UUID, List<RecipeItem>> recipeByProduct = recipes.findByProductIdIn(productIds).stream()
				.collect(Collectors.groupingBy(r -> r.getProduct().getId()));

		List<DemandForecaster.Day> history = new ArrayList<>();
		for (DailySession d : recent) {
			history.add(new DemandForecaster.Day(d.getBusinessDate(), d.getCloseType() == CloseType.AUTO,
					correctedDemand(demandBySession.getOrDefault(d.getId(), List.of()),
							summaryBySession.get(d.getId()), recipeByProduct)));
		}
		DemandForecaster.Result result = DemandForecaster.forecast(history, target);

		Map<UUID, Product> activeProducts = products
				.findByIdInAndOutletIdAndActiveTrue(result.predictions().stream()
						.map(DemandForecaster.Prediction::productId).toList(), outletId).stream()
				.collect(Collectors.toMap(Product::getId, p -> p));
		List<DemandForecaster.Prediction> predictions = result.predictions().stream()
				.filter(p -> activeProducts.containsKey(p.productId())).toList();

		forecasts.deleteFor(outletId, target);
		Map<String, Object> features = Map.of("dataPoints", result.dataPoints(), "samples", result.samples(),
				"weekdayBased", result.weekdayBased());
		forecasts.saveAll(predictions.stream().map(p -> new Forecast(outlet, target, activeProducts.get(p.productId()),
				BigDecimal.valueOf(p.predicted()), BigDecimal.valueOf(p.lower()), BigDecimal.valueOf(p.upper()),
				result.method(), MODEL_VERSION, features)).toList());

		recommendations.supersede(outletId, target);
		recommendations.saveAll(recommend(session, target, result, predictions, recipeByProduct));
	}

	/**
	 * Permintaan per produk satu sesi. Bila item utama habis, produk yang memakainya dinaikkan sebesar
	 * (terjual + terlewat) / terjual, karena permintaan sebenarnya lebih besar dari yang terjual.
	 */
	private static Map<UUID, BigDecimal> correctedDemand(List<SaleItemRepository.ProductDemand> rows,
			DailySummary summary, Map<UUID, List<RecipeItem>> recipeByProduct) {
		UUID lostItem = null;
		BigDecimal factor = BigDecimal.ONE;
		if (summary != null && summary.getLostSalesEstimate() != null) {
			Map<String, Object> lost = summary.getLostSalesEstimate();
			double qty = ((Number) lost.get("qty")).doubleValue();
			double sold = ((Number) lost.get("sold_qty")).doubleValue();
			if (qty > 0 && sold > 0) {
				lostItem = UUID.fromString((String) lost.get("stock_item_id"));
				factor = BigDecimal.valueOf(1 + qty / sold);
			}
		}
		Map<UUID, BigDecimal> demand = new HashMap<>();
		for (SaleItemRepository.ProductDemand row : rows) {
			BigDecimal qty = row.getTotal();
			final UUID item = lostItem;
			if (item != null && recipeByProduct.getOrDefault(row.getProductId(), List.of()).stream()
					.anyMatch(r -> r.getStockItem().getId().equals(item))) {
				qty = qty.multiply(factor);
			}
			demand.put(row.getProductId(), qty);
		}
		return demand;
	}

	private List<Recommendation> recommend(DailySession session, LocalDate target, DemandForecaster.Result result,
			List<DemandForecaster.Prediction> predictions, Map<UUID, List<RecipeItem>> recipeByProduct) {
		Outlet outlet = session.getOutlet();
		BigDecimal buffer = BigDecimal.ONE.add(outlet.getRecommendationBufferPct().movePointLeft(2));
		Map<UUID, SessionStock> closing = stocks.findBySessionIdOrderByStockItemName(session.getId()).stream()
				.collect(Collectors.toMap(r -> r.getStockItem().getId(), r -> r));
		Map<UUID, BigDecimal> averageCost = sessionService.latestAverageCost(outlet.getId());
		DailySummary summary = summaries.findBySessionId(session.getId()).orElse(null);
		List<Recommendation> out = new ArrayList<>();

		for (StockItem item : stockItems.findByOutletIdAndActiveTrueOrderByName(outlet.getId())) {
			if (!item.isPrimary()) {
				continue;
			}
			BigDecimal required = BigDecimal.ZERO;
			BigDecimal lowerRequired = BigDecimal.ZERO;
			BigDecimal upperRequired = BigDecimal.ZERO;
			for (DemandForecaster.Prediction p : predictions) {
				for (RecipeItem r : recipeByProduct.getOrDefault(p.productId(), List.of())) {
					if (r.getStockItem().getId().equals(item.getId())) {
						required = required.add(r.getQuantity().multiply(BigDecimal.valueOf(p.predicted())));
						lowerRequired = lowerRequired.add(r.getQuantity().multiply(BigDecimal.valueOf(p.lower())));
						upperRequired = upperRequired.add(r.getQuantity().multiply(BigDecimal.valueOf(p.upper())));
					}
				}
			}
			if (required.signum() <= 0) {
				continue;
			}
			SessionStock row = closing.get(item.getId());
			BigDecimal carry = usableCarryOver(item, row);
			BigDecimal step = UnitConverter.roundingStep(item.getUnit());
			BigDecimal recommended = need(required, buffer, carry, step);
			BigDecimal unitCost = averageCost.getOrDefault(item.getId(), BigDecimal.valueOf(item.getUnitCost()));
			out.add(new Recommendation(outlet, target, item, required, carry, recommended,
					need(lowerRequired, buffer, carry, step), need(upperRequired, buffer, carry, step),
					recommended.multiply(unitCost).setScale(0, RoundingMode.HALF_UP).longValueExact(),
					reasons(session, item, result, carry, summary, row)));
		}
		out.sort(Comparator.comparing(r -> r.getStockItem().getName()));
		return out;
	}

	/** Kebutuhan dengan buffer dikurangi sisa layak, dibulatkan ke atas, tidak pernah negatif. */
	private static BigDecimal need(BigDecimal required, BigDecimal buffer, BigDecimal carry, BigDecimal step) {
		BigDecimal net = required.multiply(buffer).subtract(carry);
		return net.signum() <= 0 ? BigDecimal.ZERO : DemandForecaster.ceilTo(net, step);
	}

	/** Sisa yang dibawa hanya dikurangkan bila besok masih dalam umur simpan bahan. */
	private static BigDecimal usableCarryOver(StockItem item, SessionStock closingRow) {
		if (closingRow == null || closingRow.getCarriedOverQty() == null
				|| closingRow.getOpeningAgeDays() + 1 > item.getShelfLifeDays()) {
			return BigDecimal.ZERO;
		}
		return closingRow.getCarriedOverQty();
	}

	private static List<Map<String, String>> reasons(DailySession session, StockItem item,
			DemandForecaster.Result result, BigDecimal carry, DailySummary summary, SessionStock closingRow) {
		List<Map<String, String>> reasons = new ArrayList<>();
		String day = InsightTexts.dayName(session.getBusinessDate().plusDays(1).toString());
		if (result.weekdayShift() > 0) {
			reasons.add(reason("WEEKDAY_UPLIFT", day + " biasanya lebih ramai"));
		}
		else if (result.weekdayShift() < 0) {
			reasons.add(reason("WEEKDAY_LOWER", day + " biasanya lebih sepi"));
		}
		Map<String, Object> lost = summary == null ? null : summary.getLostSalesEstimate();
		if (lost != null && closingRow != null && closingRow.getSoldOutAt() != null
				&& item.getId().toString().equals(lost.get("stock_item_id"))) {
			LocalTime at = closingRow.getSoldOutAt().atZone(OutletRules.zone(session.getOutlet())).toLocalTime();
			long people = Math.round(((Number) lost.get("qty")).doubleValue());
			reasons.add(reason("STOCKOUT_CORRECTION", "Kemarin habis jam " + HOUR_MINUTE.format(at)
					+ (people > 0 ? ", sekitar " + people + " pembeli terlewat" : "")));
		}
		if (carry.signum() > 0) {
			reasons.add(reason("CARRY_OVER", "Sisa " + carry.stripTrailingZeros().toPlainString() + " "
					+ item.getUnit() + " dari kemarin sudah dikurangkan"));
		}
		if (DemandForecaster.isLowData(result.dataPoints())) {
			reasons.add(reason("LOW_DATA", "Data masih sedikit (" + result.dataPoints() + " hari)"));
		}
		return reasons;
	}

	private static Map<String, String> reason(String code, String text) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("code", code);
		m.put("text", text);
		return m;
	}

}
