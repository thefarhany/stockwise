package com.thefarhany.stockwise.utils.insight;

import static org.assertj.core.api.Assertions.assertThat;

import com.thefarhany.stockwise.utils.insight.LostSalesEstimator;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class LostSalesEstimatorTest {

	private static final LocalTime OPEN = LocalTime.of(11, 0);
	private static final LocalTime CLOSE = LocalTime.of(21, 0);

	@Test
	void spreadsDemandEvenlyOverOpeningHours() {
		// terjual 10 dalam 460 menit, tersisa 140 menit: 10 x 140 / 460
		assertThat(LostSalesEstimator.estimate(BigDecimal.TEN, OPEN, CLOSE, LocalTime.of(18, 40)))
				.isEqualByComparingTo("3.0");
	}

	@Test
	void nothingLostWhenSoldOutAtOrAfterClosing() {
		assertThat(LostSalesEstimator.estimate(BigDecimal.TEN, OPEN, CLOSE, LocalTime.of(21, 0))).isEqualByComparingTo("0");
		assertThat(LostSalesEstimator.estimate(BigDecimal.TEN, OPEN, CLOSE, LocalTime.of(21, 30))).isEqualByComparingTo("0");
		assertThat(LostSalesEstimator.estimate(BigDecimal.ZERO, OPEN, CLOSE, LocalTime.of(15, 0))).isEqualByComparingTo("0");
	}

	@Test
	void veryEarlySellOutIsCappedAtSoldQuantity() {
		// habis 10 menit setelah buka: tanpa batas hasilnya ratusan
		assertThat(LostSalesEstimator.estimate(BigDecimal.TEN, OPEN, CLOSE, LocalTime.of(11, 10)))
				.isEqualByComparingTo("10.0");
	}

}
