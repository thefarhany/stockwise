package com.thefarhany.stockwise.dto.insight;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fakta yang dikirim ke model (docs/05 bagian 13): angka agregat, nama bahan, dan alasan; tanpa nama pengguna, email,
 * atau nama usaha (Privacy Policy menjanjikan itu, dan model bisa berjalan di luar server). currency = kode mata uang (mis. IDR); uang
 * ditulis dalam satuan utuh (mis. 12.50 untuk $12.50), bukan satuan terkecil, supaya model tidak salah baca.
 */
public record InsightFacts(String language, String currency, String date, String closeType,
		Facts facts) {

	public record Facts(BigDecimal revenue, BigDecimal cogs, BigDecimal netProfit, long itemsSold, int voidCount,
			PrimaryFacts primaryItem, RecommendationFacts recommendation) {
	}

	/** soldOutAt "HH:mm" menurut zona waktu usaha; null bila tidak habis. */
	public record PrimaryFacts(String name, String soldOutAt, long lostSales) {
	}

	public record RecommendationFacts(String date, String item, double qty, double lower, double upper,
			List<String> reasons) {
	}

}
