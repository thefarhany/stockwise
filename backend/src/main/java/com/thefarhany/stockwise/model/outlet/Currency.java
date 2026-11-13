package com.thefarhany.stockwise.model.outlet;

/**
 * Mata uang yang didukung. Semua uang di database dan API berupa bilangan bulat dalam satuan terkecil
 * (rupiah untuk IDR, sen untuk yang lain), jadi Rp15.000 = 15000 dan $15.00 = 1500.
 * Mata uang tidak dikonversi: mengganti mata uang hanya mengganti cara angka dibaca, karena itu dikunci setelah lapak
 * pertama dibuka.
 */
public enum Currency {

	IDR("Rp", 0, 500), MYR("RM", 2, 50), SGD("S$", 2, 50), USD("$", 2, 50), EUR("€", 2, 50);

	private final String symbol;
	private final int decimals;
	private final long priceStep;

	Currency(String symbol, int decimals, long priceStep) {
		this.symbol = symbol;
		this.decimals = decimals;
		this.priceStep = priceStep;
	}

	public String symbol() {
		return symbol;
	}

	/** Jumlah angka di belakang koma: 0 untuk IDR, 2 untuk yang lain. */
	public int decimals() {
		return decimals;
	}

	/** Kelipatan saran harga jual dalam satuan terkecil: Rp500, atau 0.50. */
	public long priceStep() {
		return priceStep;
	}

}
