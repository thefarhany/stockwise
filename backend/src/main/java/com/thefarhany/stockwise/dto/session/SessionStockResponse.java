package com.thefarhany.stockwise.dto.session;

import com.thefarhany.stockwise.model.catalog.StockUnit;
import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.utils.session.StockCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Kolom biaya (soldCost dst.) hanya untuk Owner dan baru terisi setelah sesi ditutup. */
public record SessionStockResponse(UUID stockItemId, String name, StockUnit unit, boolean isPrimary,
		BigDecimal openingQty, int openingAgeDays, BigDecimal addedQty, BigDecimal soldQty, BigDecimal adjustedQty,
		BigDecimal availableQty, BigDecimal recommendedQty, Instant soldOutAt, BigDecimal closingQty,
		BigDecimal carriedOverQty, BigDecimal discardedQty, Long soldCost, Long discardedCost, Long adjustmentCost,
		Long carriedValue) {

	public static SessionStockResponse from(SessionStock s, boolean showCost) {
		return new SessionStockResponse(s.getStockItem().getId(), s.getStockItem().getName(),
				s.getStockItem().getUnit(), s.getStockItem().isPrimary(), s.getOpeningQty(),
				s.getOpeningAgeDays(), s.getAddedQty(), s.getSoldQty(), s.getAdjustedQty(), StockCalculator.available(s),
				s.getRecommendedQty(), s.getSoldOutAt(), s.getClosingQty(), s.getCarriedOverQty(),
				s.getDiscardedQty(), showCost ? s.getSoldCost() : null, showCost ? s.getDiscardedCost() : null,
				showCost ? s.getAdjustmentCost() : null, showCost ? s.getCarriedValue() : null);
	}

}
