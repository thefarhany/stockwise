package com.thefarhany.stockwise.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Laporan satu hari dagang (sesi tertutup). Omzet dan jumlah hanya dari transaksi selesai; void dihitung terpisah.
 * byHour memakai zona waktu usaha dan hanya memuat jam yang ada penjualannya; byPaymentMethod = omzet per metode.
 */
public record DailyReportResponse(LocalDate date, long revenue, long cogs, long wasteCost, long adjustmentCost,
		long carriedValue, long expenseTotal, long netProfit, long transactions, int voidCount,
		List<ProductLine> byProduct, List<HourLine> byHour, Map<String, Long> byPaymentMethod) {

	public record ProductLine(UUID productId, String name, BigDecimal qty, long revenue) {
	}

	public record HourLine(int hour, BigDecimal qty) {
	}

}
