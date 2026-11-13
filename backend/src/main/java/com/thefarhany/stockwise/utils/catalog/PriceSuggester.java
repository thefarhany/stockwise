package com.thefarhany.stockwise.utils.catalog;

import com.thefarhany.stockwise.model.outlet.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Saran harga jual (FR-53): harga yang memberi target margin di atas HPP per porsi.
 * Margin dihitung dari harga jual: harga = HPP / (1 - margin), lalu dibulatkan ke atas ke kelipatan harga mata uang
 * (Rp500, atau 0.50) supaya mudah dipakai di warung. Contoh PRD: HPP Rp7.800 dengan margin 40% -> Rp13.000.
 */
public final class PriceSuggester {

	private PriceSuggester() {
	}

	/** null bila HPP belum ada atau nol (tidak ada dasar untuk menyarankan harga). */
	public static Long suggest(Long hpp, BigDecimal targetMarginPct, Currency currency) {
		if (hpp == null || hpp <= 0 || targetMarginPct == null) {
			return null;
		}
		BigDecimal costShare = BigDecimal.ONE.subtract(targetMarginPct.movePointLeft(2));
		long raw = BigDecimal.valueOf(hpp).divide(costShare, 0, RoundingMode.CEILING).longValueExact();
		long step = currency.priceStep();
		return (raw + step - 1) / step * step;
	}

}
