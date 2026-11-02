package com.thefarhany.stockwise.dto.session;

import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.utils.session.StockCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Stok satu bahan setelah sebuah transaksi (stockAfter di respons penjualan). */
public record StockLevelResponse(UUID stockItemId, String name, BigDecimal availableQty, Instant soldOutAt) {

	public static StockLevelResponse from(SessionStock s) {
		return new StockLevelResponse(s.getStockItem().getId(), s.getStockItem().getName(), StockCalculator.available(s),
				s.getSoldOutAt());
	}

}
