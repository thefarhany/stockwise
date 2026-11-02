package com.thefarhany.stockwise.utils.insight;

import static org.assertj.core.api.Assertions.assertThat;

import com.thefarhany.stockwise.model.insight.ForecastMethod;
import com.thefarhany.stockwise.utils.insight.DemandForecaster.Day;
import com.thefarhany.stockwise.utils.insight.DemandForecaster.Prediction;
import com.thefarhany.stockwise.utils.insight.DemandForecaster.Result;
import com.thefarhany.stockwise.utils.insight.DemandForecaster;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DemandForecasterTest {

	private static final UUID GORENG = UUID.randomUUID();
	private static final UUID ESTEH = UUID.randomUUID();

	private static Day day(String date, boolean auto, Object... productQty) {
		Map<UUID, BigDecimal> demand = new java.util.HashMap<>();
		for (int i = 0; i < productQty.length; i += 2) {
			demand.put((UUID) productQty[i], BigDecimal.valueOf(((Number) productQty[i + 1]).doubleValue()));
		}
		return new Day(LocalDate.parse(date), auto, demand);
	}

	private static Prediction of(Result r, UUID product) {
		return r.predictions().stream().filter(p -> p.productId().equals(product)).findFirst().orElseThrow();
	}

	@Test
	void noHistoryMeansNoPrediction() {
		Result r = DemandForecaster.forecast(List.of(), LocalDate.parse("2026-11-03"));
		assertThat(r.predictions()).isEmpty();
		assertThat(r.dataPoints()).isZero();
	}

	@Test
	void fewSessionsUseSimpleAverageWithWideRange() {
		Result r = DemandForecaster.forecast(List.of(day("2026-11-01", false, GORENG, 10),
				day("2026-11-02", false, GORENG, 20), day("2026-11-03", false, GORENG, 30)),
				LocalDate.parse("2026-11-04"));
		assertThat(r.method()).isEqualTo(ForecastMethod.COLD_START);
		assertThat(r.dataPoints()).isEqualTo(3);
		Prediction p = of(r, GORENG);
		assertThat(p.predicted()).isEqualTo(20);
		// simpangan relatif 50% dibatasi 30%
		assertThat(p.lower()).isEqualTo(14);
		assertThat(p.upper()).isEqualTo(26);
	}

	@Test
	void singleSessionUsesDefaultSpread() {
		Result r = DemandForecaster.forecast(List.of(day("2026-11-02", false, GORENG, 13)), LocalDate.parse("2026-11-03"));
		Prediction p = of(r, GORENG);
		assertThat(p.predicted()).isEqualTo(13);
		assertThat(p.lower()).isEqualTo(10); // 13 x 0,8 = 10,4
		assertThat(p.upper()).isEqualTo(16); // 13 x 1,2 = 15,6
	}

	@Test
	void rangeBoundsAreExactDespiteFloatingPointNoise() {
		// rata-rata 90, rentang 30%: 90 x 0,7 = 62,99999999999999 dan 90 x 1,3 = 117,00000000000001 di floating point
		Result r = DemandForecaster.forecast(List.of(day("2026-11-01", false, GORENG, 40),
				day("2026-11-02", false, GORENG, 90), day("2026-11-03", false, GORENG, 140)),
				LocalDate.parse("2026-11-04"));
		assertThat(of(r, GORENG).predicted()).isEqualTo(90);
		assertThat(of(r, GORENG).lower()).isEqualTo(63);
		assertThat(of(r, GORENG).upper()).isEqualTo(117);
	}

	@Test
	void autoClosedSessionsCountHalf() {
		Result r = DemandForecaster.forecast(List.of(day("2026-11-01", false, GORENG, 10),
				day("2026-11-02", false, GORENG, 10), day("2026-11-03", true, GORENG, 40)),
				LocalDate.parse("2026-11-04"));
		assertThat(of(r, GORENG).predicted()).isEqualTo(16); // (10 + 10 + 0,5 x 40) / 2,5
	}

	@Test
	void sessionWithoutSalesOfAProductCountsAsZero() {
		Result r = DemandForecaster.forecast(List.of(day("2026-11-01", false, GORENG, 30, ESTEH, 5),
				day("2026-11-02", false, GORENG, 30), day("2026-11-03", false, GORENG, 30)),
				LocalDate.parse("2026-11-04"));
		assertThat(of(r, ESTEH).predicted()).isEqualTo(2); // 5 / 3 = 1,67
	}

	@Test
	void baselineUsesSameWeekdayWhenEnoughSamples() {
		// 4 minggu: Senin selalu jauh lebih ramai (60) daripada hari lain (20)
		List<Day> history = new ArrayList<>();
		LocalDate d = LocalDate.parse("2026-10-05"); // Senin
		for (int i = 0; i < 28; i++) {
			LocalDate date = d.plusDays(i);
			history.add(day(date.toString(), false, GORENG, date.getDayOfWeek() == DayOfWeek.MONDAY ? 60 : 20));
		}
		Result r = DemandForecaster.forecast(history, LocalDate.parse("2026-11-02")); // Senin
		assertThat(r.method()).isEqualTo(ForecastMethod.BASELINE);
		assertThat(r.weekdayBased()).isTrue();
		assertThat(of(r, GORENG).predicted()).isEqualTo(60);
		assertThat(r.weekdayShift()).isGreaterThan(0.10);

		Result tuesday = DemandForecaster.forecast(history, LocalDate.parse("2026-11-03"));
		assertThat(of(tuesday, GORENG).predicted()).isEqualTo(20);
		assertThat(tuesday.weekdayShift()).isLessThan(-0.10);
	}

	@Test
	void baselineFallsBackToRecentSevenWhenWeekdayHasTooFewSamples() {
		List<Day> history = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			history.add(day(LocalDate.parse("2026-10-26").plusDays(i).toString(), false, GORENG, 14));
		}
		// 7 hari berurutan: hari target (Selasa) hanya punya 1 sampel pada jendela -> pakai 7 sesi terbaru
		Result r = DemandForecaster.forecast(history, LocalDate.parse("2026-11-03"));
		assertThat(r.method()).isEqualTo(ForecastMethod.BASELINE);
		assertThat(r.weekdayBased()).isFalse();
		assertThat(of(r, GORENG).predicted()).isEqualTo(14);
		assertThat(r.weekdayShift()).isZero();
	}

	@Test
	void ceilToRoundsUpToStep() {
		assertThat(DemandForecaster.ceilTo(new BigDecimal("13.65"), BigDecimal.ONE)).isEqualByComparingTo("14");
		assertThat(DemandForecaster.ceilTo(new BigDecimal("4.21"), new BigDecimal("0.1"))).isEqualByComparingTo("4.3");
		assertThat(DemandForecaster.ceilTo(new BigDecimal("4.2"), new BigDecimal("0.1"))).isEqualByComparingTo("4.2");
	}

}
