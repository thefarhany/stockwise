package com.thefarhany.stockwise.utils.insight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Perkiraan penjualan terlewat bagi item utama yang habis sebelum jam tutup.
 *
 * Rumus (milik implementasi): permintaan dianggap rata sepanjang jam operasional, jadi
 * terlewat = terjual x sisa waktu / waktu berjalan sejak buka. Waktu berjalan minimal 30 menit supaya
 * item yang habis sangat cepat tidak menghasilkan angka berlebihan, dan hasilnya dibatasi paling banyak
 * sebesar jumlah terjual (permintaan tidak diperkirakan lebih dari dua kali lipat).
 * shortcut: pola ramai per jam tidak dipakai; ganti dengan profil per jam bila data penjualan sudah banyak.
 */
public final class LostSalesEstimator {

	private static final Duration MIN_ELAPSED = Duration.ofMinutes(30);

	private LostSalesEstimator() {
	}

	/** Dalam satu desimal; 0 bila habis pada atau setelah jam tutup. */
	public static BigDecimal estimate(BigDecimal soldQty, LocalTime open, LocalTime close, LocalTime soldOutAt) {
		if (soldQty.signum() <= 0 || !soldOutAt.isBefore(close)) {
			return BigDecimal.ZERO.setScale(1);
		}
		Duration elapsed = Duration.between(open, soldOutAt);
		if (elapsed.compareTo(MIN_ELAPSED) < 0) {
			elapsed = MIN_ELAPSED;
		}
		Duration remaining = Duration.between(soldOutAt, close);
		BigDecimal lost = soldQty.multiply(BigDecimal.valueOf(remaining.toMinutes()))
				.divide(BigDecimal.valueOf(elapsed.toMinutes()), 1, RoundingMode.HALF_UP);
		return lost.min(soldQty).setScale(1, RoundingMode.HALF_UP);
	}

}
