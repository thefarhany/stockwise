package com.thefarhany.stockwise.utils.session;

import com.thefarhany.stockwise.model.session.SessionStock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Hitungan stok satu bahan dalam satu sesi, termasuk HPP (docs/04 bagian 3.8). Mengubah SessionStock yang diberikan.
 * Harga rata-rata = (nilai awal + biaya belanja) / (jumlah awal + jumlah belanja). carried_value adalah sisa, jadi
 * persamaan nilai selalu pas: opening_value + added_cost = sold_cost + discarded_cost + adjustment_cost + carried_value.
 */
public final class StockCalculator {

	private StockCalculator() {
	}

	/** Stok yang masih ada menurut catatan. */
	public static BigDecimal available(SessionStock s) {
		return s.getOpeningQty().add(s.getAddedQty()).add(s.getAdjustedQty()).subtract(s.getSoldQty());
	}

	public static void addStock(SessionStock s, BigDecimal qty, long cost) {
		s.setAddedQty(s.getAddedQty().add(qty));
		s.setAddedCost(s.getAddedCost() + cost);
		clearSoldOutIfAvailable(s);
	}

	/** Penjualan mengurangi stok; item utama yang habis dicatat jam habisnya (sekali). */
	public static void sell(SessionStock s, BigDecimal qty, Instant at) {
		s.setSoldQty(s.getSoldQty().add(qty));
		if (s.getStockItem().isPrimary() && s.getSoldOutAt() == null && available(s).signum() <= 0) {
			s.setSoldOutAt(at);
		}
	}

	/** Void mengembalikan stok; tanda habis dihapus bila stok ada lagi. */
	public static void restore(SessionStock s, BigDecimal qty) {
		s.setSoldQty(s.getSoldQty().subtract(qty));
		clearSoldOutIfAvailable(s);
	}

	/**
	 * Mengisi hitungan akhir dan semua kolom biaya sekali saat sesi ditutup.
	 *
	 * @return selisih hitungan akhir terhadap stok tercatat (+ lebih banyak, - kurang)
	 */
	public static BigDecimal applyClose(SessionStock s, BigDecimal closing, BigDecimal carryOver, BigDecimal discard) {
		BigDecimal diff = closing.subtract(available(s));
		s.setAdjustedQty(s.getAdjustedQty().add(diff));
		s.setClosingQty(closing);
		s.setCarriedOverQty(carryOver);
		s.setDiscardedQty(discard);

		long totalValue = s.getOpeningValue() + s.getAddedCost();
		BigDecimal totalQty = s.getOpeningQty().add(s.getAddedQty());
		if (totalQty.signum() == 0) {
			s.setSoldCost(0L);
			s.setDiscardedCost(0L);
			s.setAdjustmentCost(totalValue);
			s.setCarriedValue(0L);
			return diff;
		}
		BigDecimal avg = BigDecimal.valueOf(totalValue).divide(totalQty, 10, RoundingMode.HALF_UP);
		long sold = money(s.getSoldQty(), avg);
		long discarded = money(discard, avg);
		long adjustment = money(s.getAdjustedQty().negate(), avg);
		long carried = totalValue - sold - discarded - adjustment;
		// Nilai tanpa barang (atau negatif) hanya sisa pembulatan: masuk ke selisih agar tidak menumpuk di sesi berikut.
		if (carryOver.signum() == 0 || carried < 0) {
			adjustment += carried;
			carried = 0;
		}
		s.setSoldCost(sold);
		s.setDiscardedCost(discarded);
		s.setAdjustmentCost(adjustment);
		s.setCarriedValue(carried);
		return diff;
	}

	private static void clearSoldOutIfAvailable(SessionStock s) {
		if (s.getSoldOutAt() != null && available(s).signum() > 0) {
			s.setSoldOutAt(null);
		}
	}

	private static long money(BigDecimal qty, BigDecimal avg) {
		return qty.multiply(avg).setScale(0, RoundingMode.HALF_UP).longValueExact();
	}

}
