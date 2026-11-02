package com.thefarhany.stockwise.dto.insight;

import com.thefarhany.stockwise.model.catalog.StockUnit;
import com.thefarhany.stockwise.model.insight.ForecastMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Saran stok untuk satu tanggal. estimatedCost hanya untuk Owner (null untuk Cashier). */
public record RecommendationResponse(LocalDate targetDate, ForecastMethod method, int dataPoints, boolean lowData,
		List<Item> items, List<ProductForecast> products, Instant generatedAt) {

	public record Item(UUID stockItemId, String name, StockUnit unit, BigDecimal requiredQty,
			BigDecimal carryOverQty, BigDecimal recommendedQty, BigDecimal lowerQty, BigDecimal upperQty,
			Long estimatedCost, List<Reason> reasons) {
	}

	public record Reason(String code, String text) {
	}

	public record ProductForecast(UUID productId, String name, BigDecimal predictedQty, BigDecimal lowerQty,
			BigDecimal upperQty) {
	}

}
