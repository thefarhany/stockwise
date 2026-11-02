package com.thefarhany.stockwise.dto.sales;

import com.thefarhany.stockwise.dto.session.StockLevelResponse;
import com.thefarhany.stockwise.model.sales.PaymentMethod;
import com.thefarhany.stockwise.model.sales.Sale;
import com.thefarhany.stockwise.model.sales.SaleItem;
import com.thefarhany.stockwise.model.sales.SaleStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Respons setelah transaksi dibuat. change dan cashReceived null bila tidak ada uang diterima yang dicatat. */
public record SaleResponse(UUID id, int receiptNo, Instant soldAt, SaleStatus status, long totalAmount,
		PaymentMethod paymentMethod, Long cashReceived, Long change, List<SaleItemResponse> items,
		List<StockLevelResponse> stockAfter) {

	public static SaleResponse from(Sale s, List<SaleItem> items, List<StockLevelResponse> stockAfter) {
		return new SaleResponse(s.getId(), s.getReceiptNo(), s.getSoldAt(), s.getStatus(), s.getTotalAmount(),
				s.getPaymentMethod(), s.getCashReceived(), ReceiptResponse.change(s),
				items.stream().map(SaleItemResponse::from).toList(), stockAfter);
	}

}
