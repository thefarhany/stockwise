package com.thefarhany.stockwise.repository.session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/** Nilai dan jumlah total satu bahan dalam satu sesi; rata-rata = nilai / jumlah. */
public record ItemCost(UUID stockItemId, long totalValue, BigDecimal totalQty) {

	public BigDecimal average() {
		return BigDecimal.valueOf(totalValue).divide(totalQty, 10, RoundingMode.HALF_UP);
	}

}
