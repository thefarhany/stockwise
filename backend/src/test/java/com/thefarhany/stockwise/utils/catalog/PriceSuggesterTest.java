package com.thefarhany.stockwise.utils.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import static com.thefarhany.stockwise.model.outlet.Currency.IDR;
import static com.thefarhany.stockwise.model.outlet.Currency.USD;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PriceSuggesterTest {

	private static final BigDecimal FORTY = new BigDecimal("40");

	@Test
	void prdExampleGivesExactlyThirteenThousand() {
		// FR-52/53: 1 ayam Rp7.000 + 100 g tepung Rp8/g = HPP Rp7.800; margin 40% -> 7.800 / 0,6 = 13.000
		assertThat(PriceSuggester.suggest(7800L, FORTY, IDR)).isEqualTo(13000L);
	}

	@Test
	void roundsUpToTheNextFiveHundred() {
		assertThat(PriceSuggester.suggest(7960L, FORTY, IDR)).isEqualTo(13500L); // 13.266,67 -> 13.500
		assertThat(PriceSuggester.suggest(7801L, FORTY, IDR)).isEqualTo(13500L); // 13.001,67: tidak boleh turun ke 13.000
		assertThat(PriceSuggester.suggest(3000L, BigDecimal.ZERO, IDR)).isEqualTo(3000L); // margin 0 = harga pokok
		assertThat(PriceSuggester.suggest(2100L, new BigDecimal("50"), IDR)).isEqualTo(4500L); // 4.200 -> 4.500
	}

	@Test
	void decimalCurrenciesRoundUpToFiftyCents() {
		// HPP $7.80 (780 sen), margin 40% -> $13.00; $7.96 -> $13.27 -> $13.50
		assertThat(PriceSuggester.suggest(780L, FORTY, USD)).isEqualTo(1300L);
		assertThat(PriceSuggester.suggest(796L, FORTY, USD)).isEqualTo(1350L);
	}

	@Test
	void noSuggestionWithoutCostBasis() {
		assertThat(PriceSuggester.suggest(null, FORTY, IDR)).isNull();
		assertThat(PriceSuggester.suggest(0L, FORTY, IDR)).isNull(); // mis. semua bahan dari rumah (tanpa biaya)
		assertThat(PriceSuggester.suggest(7800L, null, IDR)).isNull();
	}

}
