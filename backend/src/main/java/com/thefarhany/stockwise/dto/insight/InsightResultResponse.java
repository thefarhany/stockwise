package com.thefarhany.stockwise.dto.insight;

import java.time.Instant;

public record InsightResultResponse(String status, String text, String model, int attemptsLeft,
		Instant generatedAt) {
}
