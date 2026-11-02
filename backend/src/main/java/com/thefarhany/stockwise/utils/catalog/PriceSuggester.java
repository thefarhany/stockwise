package com.thefarhany.stockwise.utils.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Saran harga jual (FR-53): harga yang memberi target margin di atas HPP per porsi.
 * Margin dihitung dari harga jual: harga = HPP / (1 - margin), lalu dibulatkan ke atas ke kelipatan Rp500
 * supaya mudah dipakai di warung. Contoh PRD: HPP Rp7.800 dengan margin 40% -> Rp13.000.
 */
public final class PriceSuggester {

	static final long ROUND_TO = 500;

	private PriceSuggester() {
	}

	/** null bila HPP belum ada atau nol (tidak ada dasar untuk menyarankan harga). */
	public static Long suggest(Long hpp, BigDecimal targetMarginPct) {
		if (hpp == null || hpp <= 0 || targetMarginPct == null) {
			return null;
		}
		BigDecimal costShare = BigDecimal.ONE.subtract(targetMarginPct.movePointLeft(2));
		long raw = BigDecimal.valueOf(hpp).divide(costShare, 0, RoundingMode.CEILING).longValueExact();
		return (raw + ROUND_TO - 1) / ROUND_TO * ROUND_TO;
	}

}
