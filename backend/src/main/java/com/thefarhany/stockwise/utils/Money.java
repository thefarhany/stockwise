package com.thefarhany.stockwise.utils;

/**
 * Batas atas isian uang (satuan terkecil mata uang: rupiah, sen) dan jumlah di API. Tanpa batas, hasil kali harga x jumlah atau jumlah beberapa baris
 * bisa meluap (long) diam-diam menjadi angka negatif. Dengan batas ini: harga x jumlah (1 miliar x 100.000) x 100 baris
 * tetap jauh di bawah batas long. Angka ini jauh di atas kebutuhan warung.
 */
public final class Money {

	/** Harga jual per porsi dan harga perkiraan per satuan bahan: Rp1 miliar (atau 10 juta dalam mata uang bersen). */
	public static final long MAX_UNIT_PRICE = 1_000_000_000L;

	/** Total belanja satu baris dan uang diterima satu transaksi: Rp1 triliun. */
	public static final long MAX_AMOUNT = 1_000_000_000_000L;

	/** Jumlah bahan per porsi di resep (dalam satuan bahan, mis. gram). */
	public static final String MAX_RECIPE_QUANTITY = "1000000";

	private Money() {
	}

}
