package com.thefarhany.stockwise.model.catalog;

/**
 * Satuan bahan. Satuan sekeluarga bisa saling dikonversi lewat factor (kelipatan satuan dasar keluarga:
 * gram, ml, atau pcs). Satuan tanpa keluarga hanya bisa dikonversi ke dirinya sendiri.
 */
public enum StockUnit {
	// Berat, dasar gram.
	GRAM(Family.MASS, 1), ONS(Family.MASS, 100), KG(Family.MASS, 1000),
	// Volume, dasar ml. Cup dikunci 240 ml.
	ML(Family.VOLUME, 1), SDT(Family.VOLUME, 5), SDM(Family.VOLUME, 15), CUP(Family.VOLUME, 240), LITER(Family.VOLUME, 1000),
	// Hitungan berisi tetap, dasar pcs.
	PCS(Family.COUNT, 1), BUTIR(Family.COUNT, 1), BUAH(Family.COUNT, 1), LUSIN(Family.COUNT, 12), KODI(Family.COUNT, 20),
	TRAY(Family.COUNT, 30),
	// Hitungan bebas dan kemasan: isinya beda per barang, jadi tidak dikonversi.
	SIUNG, IKAT, BATANG, LEMBAR, EKOR, POTONG, SISIR, PORSI,
	BUNGKUS, PACK, BOTOL, KALENG, GALON, JERIGEN;

	public enum Family {
		MASS, VOLUME, COUNT
	}

	private final Family family;
	private final int factor;

	StockUnit(Family family, int factor) {
		this.family = family;
		this.factor = factor;
	}

	StockUnit() {
		this(null, 1);
	}

	public Family family() {
		return family;
	}

	public int factor() {
		return factor;
	}
}
