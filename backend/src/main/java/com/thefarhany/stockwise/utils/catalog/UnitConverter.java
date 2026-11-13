package com.thefarhany.stockwise.utils.catalog;

import com.thefarhany.stockwise.model.catalog.StockUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Konversi satuan pasar ke satuan bahan dalam satu keluarga (berat, volume, hitungan), lewat StockUnit.factor. */
public final class UnitConverter {

	private static final BigDecimal LARGE_UNIT_STEP = new BigDecimal("0.1");

	private UnitConverter() {
	}

	/** Satuan sama selalu boleh; selain itu hanya dalam keluarga yang sama. */
	public static boolean convertible(StockUnit from, StockUnit to) {
		return from == to || (from.family() != null && from.family() == to.family());
	}

	/** Hasil dibulatkan ke 3 desimal, sesuai DECIMAL(12,3). */
	public static BigDecimal convert(BigDecimal quantity, StockUnit from, StockUnit to) {
		if (!convertible(from, to)) {
			throw new IllegalArgumentException(from + " tidak bisa dikonversi ke " + to);
		}
		if (from == to) {
			return quantity;
		}
		return quantity.multiply(BigDecimal.valueOf(from.factor())).divide(BigDecimal.valueOf(to.factor()), 3, RoundingMode.HALF_UP);
	}

	/** Kelipatan pembulatan jumlah saran: satuan besar (KG, LITER) per 0,1, selain itu bilangan bulat. */
	public static BigDecimal roundingStep(StockUnit unit) {
		return unit == StockUnit.KG || unit == StockUnit.LITER ? LARGE_UNIT_STEP : BigDecimal.ONE;
	}

}
