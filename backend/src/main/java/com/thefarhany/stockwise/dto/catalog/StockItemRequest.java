package com.thefarhany.stockwise.dto.catalog;

import com.thefarhany.stockwise.model.catalog.StockKind;
import com.thefarhany.stockwise.model.catalog.StockUnit;
import com.thefarhany.stockwise.utils.Money;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Field opsional memakai default (harga 0, umur 1 hari, boleh dibawa, bukan item utama). active hanya dipakai PUT. */
public record StockItemRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull StockKind kind,
		@NotNull StockUnit unit,
		@PositiveOrZero @Max(Money.MAX_UNIT_PRICE) Long unitCost,
		@Min(1) @Max(365) Integer shelfLifeDays,
		Boolean carryOverAllowed,
		Boolean isPrimary,
		Boolean active) {

	public StockItemRequest {
		unitCost = unitCost == null ? 0L : unitCost;
		shelfLifeDays = shelfLifeDays == null ? 1 : shelfLifeDays;
		carryOverAllowed = carryOverAllowed == null ? Boolean.TRUE : carryOverAllowed;
		isPrimary = isPrimary == null ? Boolean.FALSE : isPrimary;
	}

}
