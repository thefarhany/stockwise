package com.thefarhany.stockwise.model.catalog;

/**
 * Satuan bahan. Satuan sekeluarga bisa saling dikonversi lewat factor (kelipatan satuan dasar keluarga:
 * gram, ml, atau pcs). Satuan tanpa keluarga hanya bisa dikonversi ke dirinya sendiri.
 */
public enum StockUnit {
	// Berat, dasar gram.
	GRAM(Family.MASS, 1), KG(Family.MASS, 1000),
	// Volume, dasar ml.
	ML(Family.VOLUME, 1), LITER(Family.VOLUME, 1000),
	// Hitungan, dasar pcs. Tray dikunci 30 (tray telur).
	PCS(Family.COUNT, 1), TRAY(Family.COUNT, 30),
	// Kemasan: isinya beda per barang, jadi tidak dikonversi.
	BOTOL, PACK, DUS, RENCENG;

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
