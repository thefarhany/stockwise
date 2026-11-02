package com.thefarhany.stockwise.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tren omzet, HPP, terbuang, dan laba (FR-26). Hanya sesi tertutup. points hanya memuat periode yang punya sesi;
 * untuk groupBy=week, period = hari Senin awal minggu itu (minggu ISO). total = jumlah seluruh rentang.
 */
public record RangeReportResponse(LocalDate from, LocalDate to, String groupBy, List<Point> points, Totals total) {

	public record Point(LocalDate period, long revenue, long cogs, long wasteCost, long netProfit,
			BigDecimal itemsSold, int sessions) {
	}

	public record Totals(long revenue, long cogs, long wasteCost, long netProfit, BigDecimal itemsSold,
			int sessions) {
	}

}
