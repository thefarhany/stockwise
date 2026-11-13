package com.thefarhany.stockwise.dto.session;

import com.thefarhany.stockwise.model.catalog.StockUnit;
import com.thefarhany.stockwise.utils.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Satu baris belanja/produksi. Wajib tepat satu dari: totalCost (> 0) atau noCost = true (stok dari rumah).
 * unit (opsional) = satuan pasar yang sekeluarga dengan satuan bahan (lihat StockUnit, mis. KG->GRAM, TRAY->BUTIR).
 */
public record StockLineRequest(
		@NotNull UUID stockItemId,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
		StockUnit unit,
		@Max(Money.MAX_AMOUNT) Long totalCost,
		Boolean noCost) {
}
