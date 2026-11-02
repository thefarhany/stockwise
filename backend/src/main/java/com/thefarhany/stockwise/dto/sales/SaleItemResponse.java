package com.thefarhany.stockwise.dto.sales;

import com.thefarhany.stockwise.model.sales.SaleItem;
import java.util.UUID;

public record SaleItemResponse(UUID productId, String name, int quantity, long unitPrice, long subtotal) {

	public static SaleItemResponse from(SaleItem i) {
		return new SaleItemResponse(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity().intValue(),
				i.getUnitPrice(), i.getSubtotal());
	}

}
