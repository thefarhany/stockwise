package com.thefarhany.stockwise.service.insight;

import com.thefarhany.stockwise.dto.insight.RecommendationResponse;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.auth.Role;
import com.thefarhany.stockwise.model.insight.Forecast;
import com.thefarhany.stockwise.model.insight.ForecastMethod;
import com.thefarhany.stockwise.model.insight.Recommendation;
import com.thefarhany.stockwise.model.insight.RecommendationStatus;
import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.repository.insight.ForecastRepository;
import com.thefarhany.stockwise.repository.insight.RecommendationRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.repository.session.SessionStockRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.utils.insight.DemandForecaster;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Memakai saran yang sudah dibuat ForecastService saat sesi ditutup: melayani GET /recommendations dan menyalin
 * saran ke stok sesi yang baru dibuka. Hanya membaca saran; tidak ada hitungan prediksi di sini.
 */
@Service
public class RecommendationService {

	private final ForecastRepository forecasts;
	private final RecommendationRepository recommendations;
	private final SessionStockRepository stocks;
	private final OutletRepository outlets;
	private final Clock clock;

	public RecommendationService(ForecastRepository forecasts, RecommendationRepository recommendations,
			SessionStockRepository stocks, OutletRepository outlets, Clock clock) {
		this.forecasts = forecasts;
		this.recommendations = recommendations;
		this.stocks = stocks;
		this.outlets = outlets;
		this.clock = clock;
	}

	/** Menyalin saran untuk tanggal sesi ke baris stok sesi yang baru dibuka (untuk mengukur akurasi nanti). */
	@Transactional
	public void applyToSession(DailySession session) {
		Map<UUID, BigDecimal> saran = recommendations
				.findByOutletIdAndTargetDateAndStatus(session.getOutlet().getId(), session.getBusinessDate(),
						RecommendationStatus.ACTIVE).stream()
				.collect(Collectors.toMap(r -> r.getStockItem().getId(), Recommendation::getRecommendedQty));
		for (SessionStock row : stocks.findBySessionIdOrderByStockItemName(session.getId())) {
			BigDecimal qty = saran.get(row.getStockItem().getId());
			if (qty != null) {
				row.setRecommendedQty(qty);
			}
		}
	}

	@Transactional(readOnly = true)
	public RecommendationResponse get(AuthPrincipal me, LocalDate date) {
		UUID outletId = me.outletId();
		LocalDate day = date != null ? date : OutletRules.today(outlets.findById(outletId).orElseThrow(), clock);
		List<Recommendation> recs = recommendations.findByOutletIdAndTargetDateAndStatus(outletId, day,
				RecommendationStatus.ACTIVE);
		List<Forecast> fcs = forecasts.findByOutletIdAndTargetDate(outletId, day);
		if (recs.isEmpty() && fcs.isEmpty()) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Belum ada saran untuk tanggal ini.");
		}
		boolean owner = me.role() == Role.OWNER;
		Map<String, Object> features = fcs.isEmpty() ? Map.of() : fcs.get(0).getFeatures();
		int dataPoints = features.get("dataPoints") instanceof Number n ? n.intValue() : 0;
		List<RecommendationResponse.Item> items = recs.stream().map(r -> new RecommendationResponse.Item(
				r.getStockItem().getId(), r.getStockItem().getName(), r.getStockItem().getUnit(), r.getRequiredQty(),
				r.getCarryOverQty(), r.getRecommendedQty(), r.getLowerQty(), r.getUpperQty(),
				owner ? r.getEstimatedCost() : null,
				r.getReasons().stream().map(m -> new RecommendationResponse.Reason(m.get("code"), m.get("text")))
						.toList())).toList();
		List<RecommendationResponse.ProductForecast> prods = fcs.stream()
				.sorted(Comparator.comparing(f -> f.getProduct().getName()))
				.map(f -> new RecommendationResponse.ProductForecast(f.getProduct().getId(), f.getProduct().getName(),
						f.getPredictedQty(), f.getLowerQty(), f.getUpperQty())).toList();
		return new RecommendationResponse(day, fcs.isEmpty() ? ForecastMethod.COLD_START : fcs.get(0).getMethod(),
				dataPoints, DemandForecaster.isLowData(dataPoints), items, prods,
				recs.stream().map(Recommendation::getCreatedAt).max(Comparator.naturalOrder())
						.orElse(null));
	}

}
