package com.thefarhany.stockwise.dto.insight;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Angka ringkasan malam, tersedia begitu sesi ditutup. netProfit = revenue - cogs - wasteCost - adjustmentCost -
 * expenseTotal; di R1 expenseTotal selalu 0, jadi ini laba sebelum biaya operasional.
 */
public record SummaryResponse(UUID sessionId, LocalDate businessDate, String closeType, long revenue, long cogs,
		long wasteCost, long adjustmentCost, long carriedValue, long expenseTotal, long netProfit,
		BigDecimal totalItemsSold, int voidCount, PrimaryItem primaryItem, InsightBlock insight,
		LocalDate nextDayRecommendationDate) {

	/** lostSalesEstimate dan soldOutAt null bila item utama tidak habis. */
	public record PrimaryItem(UUID stockItemId, String name, BigDecimal soldQty, Instant soldOutAt,
			BigDecimal lostSalesEstimate) {
	}

	public record InsightBlock(String status, String text, String model, int attemptsLeft, Instant generatedAt) {
	}

}
