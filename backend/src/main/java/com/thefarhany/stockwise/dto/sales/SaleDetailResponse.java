package com.thefarhany.stockwise.dto.sales;

import com.thefarhany.stockwise.model.sales.Sale;
import com.thefarhany.stockwise.model.sales.SaleItem;
import com.thefarhany.stockwise.model.sales.SaleStatus;
import java.util.List;
import java.util.UUID;

public record SaleDetailResponse(UUID id, SaleStatus status, ReceiptResponse receipt, String voidReason) {

	public static SaleDetailResponse from(Sale s, List<SaleItem> items) {
		ReceiptResponse receipt = new ReceiptResponse(s.getOutlet().getName(), s.getReceiptNo(), s.getSoldAt(),
				s.getCashier().getName(),
				items.stream().map(i -> new ReceiptResponse.ReceiptItem(i.getProduct().getName(), i.getQuantity().intValue(),
						i.getUnitPrice(), i.getSubtotal())).toList(),
				s.getTotalAmount(), s.getPaymentMethod(), s.getCashReceived(), ReceiptResponse.change(s));
		return new SaleDetailResponse(s.getId(), s.getStatus(), receipt, s.getVoidReason());
	}

}
