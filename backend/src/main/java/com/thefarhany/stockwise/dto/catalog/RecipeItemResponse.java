package com.thefarhany.stockwise.dto.catalog;

import com.thefarhany.stockwise.model.catalog.RecipeItem;
import com.thefarhany.stockwise.model.catalog.StockUnit;
import java.math.BigDecimal;
import java.util.UUID;

public record RecipeItemResponse(UUID stockItemId, String stockItemName, StockUnit unit, BigDecimal quantity) {

	public static RecipeItemResponse from(RecipeItem r) {
		return new RecipeItemResponse(r.getStockItem().getId(), r.getStockItem().getName(), r.getStockItem().getUnit(),
				r.getQuantity());
	}

}
