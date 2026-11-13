package com.thefarhany.stockwise.utils.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thefarhany.stockwise.model.catalog.StockUnit;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UnitConverterTest {

	@Test
	void convertsWithinFamily() {
		assertThat(UnitConverter.convert(new BigDecimal("15"), StockUnit.KG, StockUnit.GRAM)).isEqualByComparingTo("15000");
		assertThat(UnitConverter.convert(new BigDecimal("250"), StockUnit.GRAM, StockUnit.KG)).isEqualByComparingTo("0.25");
		assertThat(UnitConverter.convert(new BigDecimal("1.5"), StockUnit.LITER, StockUnit.ML)).isEqualByComparingTo("1500");
		assertThat(UnitConverter.convert(BigDecimal.ONE, StockUnit.ML, StockUnit.LITER)).isEqualByComparingTo("0.001");
		assertThat(UnitConverter.convert(new BigDecimal("3"), StockUnit.PCS, StockUnit.PCS)).isEqualByComparingTo("3");
		assertThat(UnitConverter.convert(new BigDecimal("2"), StockUnit.TRAY, StockUnit.PCS)).isEqualByComparingTo("60");
		assertThat(UnitConverter.convert(new BigDecimal("10"), StockUnit.PCS, StockUnit.TRAY)).isEqualByComparingTo("0.333");
	}

	@Test
	void rejectsOtherFamilies() {
		assertThat(UnitConverter.convertible(StockUnit.KG, StockUnit.ML)).isFalse();
		assertThat(UnitConverter.convertible(StockUnit.PCS, StockUnit.GRAM)).isFalse();
		assertThat(UnitConverter.convertible(StockUnit.RENCENG, StockUnit.PCS)).isFalse();
		assertThat(UnitConverter.convertible(StockUnit.DUS, StockUnit.PACK)).isFalse();
		assertThat(UnitConverter.convertible(StockUnit.BOTOL, StockUnit.BOTOL)).isTrue();
		assertThatThrownBy(() -> UnitConverter.convert(BigDecimal.ONE, StockUnit.KG, StockUnit.LITER))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void largeUnitsRoundByTenthsOthersByWholeNumbers() {
		assertThat(UnitConverter.roundingStep(StockUnit.KG)).isEqualByComparingTo("0.1");
		assertThat(UnitConverter.roundingStep(StockUnit.LITER)).isEqualByComparingTo("0.1");
		assertThat(UnitConverter.roundingStep(StockUnit.PCS)).isEqualByComparingTo("1");
		assertThat(UnitConverter.roundingStep(StockUnit.GRAM)).isEqualByComparingTo("1");
	}

}
