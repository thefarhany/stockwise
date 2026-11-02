package com.thefarhany.stockwise.utils.insight;

import com.thefarhany.stockwise.model.insight.ForecastMethod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Prediksi permintaan per produk (tahap Awal dan Dasar di PRD 6.6). Murni hitungan, tanpa database.
 *
 * Aturan (rumus ini milik implementasi; dokumen spesifikasi forecast belum ada):
 * - Jendela = 28 sesi tertutup terbaru. Kurang dari 7 sesi: COLD_START, rata-rata semua sesi.
 * - 7 sesi atau lebih: BASELINE. Bila ada minimal 2 sesi pada hari yang sama dalam seminggu dengan tanggal target,
 *   pakai maksimal 4 yang terbaru; jika tidak, pakai 7 sesi terbaru.
 * - Sesi tutup otomatis berbobot 0,5 (sisa stoknya tidak dihitung langsung); sesi manual berbobot 1.
 * - Sesi tanpa penjualan sebuah produk dihitung 0 untuk produk itu.
 * - Rentang = prediksi dikali (1 -/+ spread). Spread = simpangan baku relatif (dibatasi 8%-30%) bila sampel >= 3,
 *   selain itu 20%.
 * - 21 sesi atau lebih tetap BASELINE; model forecasting lanjutan (ML) ada di R3.
 */
public final class DemandForecaster {

	public static final int WINDOW = 28;
	static final int COLD_START_BELOW = 7;
	static final double AUTO_WEIGHT = 0.5;
	static final double DEFAULT_SPREAD = 0.20;
	static final double MIN_SPREAD = 0.08;
	static final double MAX_SPREAD = 0.30;
	static final double WEEKDAY_SHIFT = 0.10;
	private static final double EPSILON = 1e-9;

	/** Satu sesi tertutup: permintaan per produk (sudah dikoreksi penjualan terlewat). */
	public record Day(LocalDate date, boolean auto, Map<UUID, BigDecimal> demand) {
	}

	public record Prediction(UUID productId, int predicted, int lower, int upper) {
	}

	/** weekdayShift: >0 hari target lebih ramai dari rata-rata, <0 lebih sepi, 0 bila tidak ada pembanding. */
	public record Result(ForecastMethod method, int dataPoints, int samples, boolean weekdayBased,
			double weekdayShift, List<Prediction> predictions) {
	}

	private DemandForecaster() {
	}

	/** Data masih sedikit (kurang dari 7 sesi): saran memakai metode awal dan diberi label. */
	public static boolean isLowData(int dataPoints) {
		return dataPoints < COLD_START_BELOW;
	}

	public static Result forecast(List<Day> history, LocalDate target) {
		List<Day> window = history.stream().sorted(Comparator.comparing(Day::date).reversed()).limit(WINDOW).toList();
		if (window.isEmpty()) {
			return new Result(ForecastMethod.COLD_START, 0, 0, false, 0, List.of());
		}
		DayOfWeek dow = target.getDayOfWeek();
		List<Day> sameWeekday = window.stream().filter(d -> d.date().getDayOfWeek() == dow).limit(4).toList();

		ForecastMethod method;
		List<Day> samples;
		boolean weekdayBased = false;
		if (window.size() < COLD_START_BELOW) {
			method = ForecastMethod.COLD_START;
			samples = window;
		}
		else {
			method = ForecastMethod.BASELINE;
			if (sameWeekday.size() >= 2) {
				samples = sameWeekday;
				weekdayBased = true;
			}
			else {
				samples = window.subList(0, COLD_START_BELOW);
			}
		}

		Set<UUID> products = new LinkedHashSet<>();
		samples.forEach(d -> products.addAll(d.demand().keySet()));
		List<Prediction> predictions = new ArrayList<>();
		for (UUID product : products) {
			double[] values = samples.stream().mapToDouble(d -> d.demand().getOrDefault(product, BigDecimal.ZERO)
					.doubleValue()).toArray();
			double[] weights = samples.stream().mapToDouble(d -> d.auto() ? AUTO_WEIGHT : 1.0).toArray();
			double mean = weightedMean(values, weights);
			if (mean <= 0) {
				continue;
			}
			double spread = values.length >= 3 ? Math.min(MAX_SPREAD, Math.max(MIN_SPREAD, stdDev(values) / mean))
					: DEFAULT_SPREAD;
			int predicted = (int) Math.round(mean);
			// toleransi kecil: 5 x 1,2 jangan menjadi 6,0000000000001 lalu dibulatkan ke 7
			int lower = Math.min(predicted, (int) Math.floor(mean * (1 - spread) + EPSILON));
			int upper = Math.max(predicted, (int) Math.ceil(mean * (1 + spread) - EPSILON));
			predictions.add(new Prediction(product, predicted, lower, upper));
		}
		return new Result(method, window.size(), samples.size(), weekdayBased, weekdayShift(window, sameWeekday),
				predictions);
	}

	/** Selisih relatif total permintaan hari yang sama terhadap semua hari; 0 bila datanya belum memadai. */
	private static double weekdayShift(List<Day> window, List<Day> sameWeekday) {
		if (window.size() < COLD_START_BELOW || sameWeekday.size() < 2) {
			return 0;
		}
		double all = window.stream().mapToDouble(DemandForecaster::total).average().orElse(0);
		double same = sameWeekday.stream().mapToDouble(DemandForecaster::total).average().orElse(0);
		if (all <= 0) {
			return 0;
		}
		double shift = same / all - 1;
		return Math.abs(shift) >= WEEKDAY_SHIFT ? shift : 0;
	}

	private static double total(Day d) {
		return d.demand().values().stream().mapToDouble(BigDecimal::doubleValue).sum();
	}

	private static double weightedMean(double[] values, double[] weights) {
		double sum = 0;
		double w = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i] * weights[i];
			w += weights[i];
		}
		return w == 0 ? 0 : sum / w;
	}

	private static double stdDev(double[] values) {
		double mean = java.util.Arrays.stream(values).average().orElse(0);
		double variance = java.util.Arrays.stream(values).map(v -> (v - mean) * (v - mean)).sum() / (values.length - 1);
		return Math.sqrt(variance);
	}

	/** Pembulatan ke atas ke kelipatan langkah (satuan besar 0,1; lainnya bilangan bulat). */
	public static BigDecimal ceilTo(BigDecimal qty, BigDecimal step) {
		return qty.divide(step, 0, RoundingMode.CEILING).multiply(step);
	}

}
