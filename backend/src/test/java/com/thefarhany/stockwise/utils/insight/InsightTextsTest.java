package com.thefarhany.stockwise.utils.insight;

import static org.assertj.core.api.Assertions.assertThat;

import com.thefarhany.stockwise.dto.insight.InsightFacts.Facts;
import com.thefarhany.stockwise.dto.insight.InsightFacts.PrimaryFacts;
import com.thefarhany.stockwise.dto.insight.InsightFacts.RecommendationFacts;
import com.thefarhany.stockwise.dto.insight.InsightFacts;
import com.thefarhany.stockwise.utils.insight.InsightTexts;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class InsightTextsTest {

	private static InsightFacts facts(String closeType, PrimaryFacts primary, RecommendationFacts rec) {
		return new InsightFacts("id", "Geprek Pak Budi", "2026-10-31", closeType,
				new Facts(1620000, 982800, 637200, 108, 0, primary, rec));
	}

	private static final PrimaryFacts SOLD_OUT = new PrimaryFacts("Ayam", "18:40", 12);
	private static final RecommendationFacts REC = new RecommendationFacts("2026-11-01", "Ayam", 115, 108, 122,
			List.of("Sabtu biasanya lebih ramai", "Kemarin habis jam 18.40"));

	private static Set<String> allowed(InsightFacts f) {
		return InsightTexts.allowedNumbers(JsonMapper.builder().build().writeValueAsString(f));
	}

	@Test
	void formatsRupiahWithDots() {
		assertThat(InsightTexts.rupiah(637200)).isEqualTo("Rp637.200");
		assertThat(InsightTexts.rupiah(1620000)).isEqualTo("Rp1.620.000");
		assertThat(InsightTexts.rupiah(0)).isEqualTo("Rp0");
	}

	@Test
	void templateUsesOnlyNumbersFromFacts() {
		for (InsightFacts f : List.of(facts("MANUAL", SOLD_OUT, REC), facts("AUTO", SOLD_OUT, REC),
				facts("MANUAL", null, null), facts("MANUAL", new PrimaryFacts("Ayam", null, 0), REC),
				facts("MANUAL", new PrimaryFacts("Ayam", "18:40", 0), REC))) {
			String text = InsightTexts.template(f);
			assertThat(accepts(text, allowed(f))).as(text).isTrue();
		}
		String text = InsightTexts.template(facts("MANUAL", SOLD_OUT, REC));
		assertThat(text).contains("108 porsi", "Rp637.200", "18.40", "12 pembeli", "115 Ayam", "108 sampai 122");
	}

	@Test
	void autoClosedTemplateSaysStockWasCountedAutomatically() {
		assertThat(InsightTexts.template(facts("AUTO", SOLD_OUT, REC))).contains("otomatis");
		assertThat(InsightTexts.template(facts("MANUAL", SOLD_OUT, REC))).doesNotContain("otomatis");
	}

	@Test
	void acceptsEquivalentNumberNotations() {
		Set<String> ok = allowed(facts("MANUAL", SOLD_OUT, REC));
		assertThat(accepts("Omzet Rp1.620.000 dan laba Rp637.200.", ok)).isTrue();
		assertThat(accepts("Omzet 1620000, laba 637200.", ok)).isTrue();
		assertThat(accepts("Ayam habis jam 18.40 dan 18:40.", ok)).isTrue();
		assertThat(accepts("Siapkan 115 potong, kisaran 108 sampai 122.", ok)).isTrue();
		assertThat(accepts("Tanpa angka sama sekali.", ok)).isTrue();
	}

	@Test
	void rejectsInventedOrMisreadNumbers() {
		Set<String> ok = allowed(facts("MANUAL", SOLD_OUT, REC));
		assertThat(accepts("Laba Rp637.201.", ok)).isFalse();
		assertThat(accepts("Laku 109 porsi.", ok)).isFalse();
		assertThat(accepts("Habis jam 18.45.", ok)).isFalse();
		assertThat(accepts("Omzet Rp1,6 juta.", ok)).isFalse();
		assertThat(accepts("Laba naik 15 persen.", ok)).isFalse();
	}

	@Test
	void rejectionExplainsWhyATextIsNotUsable() {
		Set<String> ok = allowed(facts("MANUAL", SOLD_OUT, REC));
		assertThat(InsightTexts.rejection(null, ok)).isEqualTo("empty");
		assertThat(InsightTexts.rejection("   ", ok)).isEqualTo("empty");
		assertThat(InsightTexts.rejection("a".repeat(InsightTexts.MAX_LENGTH + 1), ok)).isEqualTo("too_long");
		assertThat(InsightTexts.rejection("Laku 109 porsi, laba Rp637.201.", ok))
				.isEqualTo("unknown_numbers=[109, 637201]");
		assertThat(InsightTexts.rejection("Hari yang baik.", ok)).isNull();
	}

	/** Teks boleh dipakai bila semua angkanya ada di fakta. */
	private static boolean accepts(String text, Set<String> allowed) {
		return InsightTexts.rejection(text, allowed) == null;
	}

}
