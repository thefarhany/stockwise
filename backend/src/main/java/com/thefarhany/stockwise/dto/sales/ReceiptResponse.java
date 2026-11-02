package com.thefarhany.stockwise.dto.sales;

import com.thefarhany.stockwise.model.sales.PaymentMethod;
import com.thefarhany.stockwise.model.sales.Sale;
import java.time.Instant;
import java.util.List;

/** Data struk. Label SALINAN / BATAL dan lebar kertas ditentukan client. */
public record ReceiptResponse(String businessName, int receiptNo, Instant soldAt, String cashierName,
		List<ReceiptItem> items, long totalAmount, PaymentMethod paymentMethod, Long cashReceived, Long change) {

	/** Kembalian untuk tunai; null bila bukan tunai atau uang diterima tidak dicatat. */
	static Long change(Sale sale) {
		return sale.getPaymentMethod() == PaymentMethod.CASH && sale.getCashReceived() != null
				? sale.getCashReceived() - sale.getTotalAmount() : null;
	}

	public record ReceiptItem(String name, int quantity, long unitPrice, long subtotal) {
	}

}
