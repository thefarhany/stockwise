package com.thefarhany.stockwise.dto.sales;

import com.thefarhany.stockwise.model.sales.PaymentMethod;
import com.thefarhany.stockwise.model.sales.Sale;
import com.thefarhany.stockwise.model.sales.SaleStatus;
import java.time.Instant;
import java.util.UUID;

/** Satu baris riwayat transaksi; itemCount = jumlah porsi. */
public record SaleSummaryResponse(UUID id, int receiptNo, Instant soldAt, long totalAmount,
		PaymentMethod paymentMethod, SaleStatus status, String cashierName, int itemCount) {

	public static SaleSummaryResponse from(Sale s, int itemCount) {
		return new SaleSummaryResponse(s.getId(), s.getReceiptNo(), s.getSoldAt(), s.getTotalAmount(),
				s.getPaymentMethod(), s.getStatus(), s.getCashier().getName(), itemCount);
	}

}
