package com.thefarhany.stockwise.dto.insight;

import java.util.List;

/**
 * Fakta yang dikirim ke model (docs/05 bagian 13): hanya angka agregat dan alasan, tanpa nama pengguna atau email.
 * businessName adalah nama usaha untuk sapaan, bukan identitas pribadi.
 */
public record InsightFacts(String language, String businessName, String date, String closeType, Facts facts) {

	public record Facts(long revenue, long cogs, long netProfit, long itemsSold, int voidCount,
			PrimaryFacts primaryItem, RecommendationFacts recommendation) {
	}

	/** soldOutAt "HH:mm" menurut zona waktu usaha; null bila tidak habis. */
	public record PrimaryFacts(String name, String soldOutAt, long lostSales) {
	}

	public record RecommendationFacts(String date, String item, double qty, double lower, double upper,
			List<String> reasons) {
	}

}
