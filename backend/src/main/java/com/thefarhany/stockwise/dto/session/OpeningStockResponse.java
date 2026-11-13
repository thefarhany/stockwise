package com.thefarhany.stockwise.dto.session;

import com.thefarhany.stockwise.model.catalog.StockItem;
import com.thefarhany.stockwise.model.catalog.StockUnit;
import com.thefarhany.stockwise.model.session.SessionStock;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stok satu bahan bila lapak dibuka sekarang: sisa sesi tutup terakhir yang dibawa (0 bila tidak ada), umurnya nanti
 * (hari ke-berapa sejak dibuat/dibeli), dan apakah bahan masih aktif.
 */
public record OpeningStockResponse(UUID stockItemId, String name, StockUnit unit, boolean isPrimary, boolean active,
		BigDecimal quantity, int ageDays) {

	/** previous = baris sesi tutup terakhir yang sisanya dibawa, atau null. */
	public static OpeningStockResponse of(StockItem item, SessionStock previous) {
		return new OpeningStockResponse(item.getId(), item.getName(), item.getUnit(), item.isPrimary(), item.isActive(),
				previous == null ? BigDecimal.ZERO : previous.getCarriedOverQty(),
				previous == null ? 0 : previous.getOpeningAgeDays() + 1);
	}

}
