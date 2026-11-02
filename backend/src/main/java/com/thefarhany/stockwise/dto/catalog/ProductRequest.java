package com.thefarhany.stockwise.dto.catalog;

import com.thefarhany.stockwise.model.catalog.StockMode;
import com.thefarhany.stockwise.utils.Money;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** stockMode default MADE_TO_ORDER; BATCH ditolak di R1. active hanya dipakai PUT. */
public record ProductRequest(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 50) String category,
		@NotNull @PositiveOrZero @Max(Money.MAX_UNIT_PRICE) Long price,
		StockMode stockMode,
		@Min(0) @Max(32000) Integer sortOrder,
		Boolean active) {

	public ProductRequest {
		stockMode = stockMode == null ? StockMode.MADE_TO_ORDER : stockMode;
		sortOrder = sortOrder == null ? 0 : sortOrder;
	}

}
