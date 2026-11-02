package com.thefarhany.stockwise.dto.catalog;

import com.thefarhany.stockwise.model.catalog.StockItem;
import com.thefarhany.stockwise.model.catalog.StockKind;
import com.thefarhany.stockwise.model.catalog.StockUnit;
import java.util.UUID;

/** unitCost null untuk Cashier (nilai biaya hanya untuk Owner). */
public record StockItemResponse(UUID id, String name, StockKind kind, StockUnit unit, Long unitCost,
		int shelfLifeDays, boolean carryOverAllowed, boolean isPrimary, boolean active) {

	public static StockItemResponse from(StockItem s, boolean showCost) {
		return new StockItemResponse(s.getId(), s.getName(), s.getKind(), s.getUnit(),
				showCost ? s.getUnitCost() : null, s.getShelfLifeDays(), s.isCarryOverAllowed(), s.isPrimary(),
				s.isActive());
	}

}
