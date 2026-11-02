package com.thefarhany.stockwise.dto.sales;

import com.thefarhany.stockwise.model.sales.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Rekap akhir hari: total terjual per produk, disimpan sebagai satu transaksi. */
public record RecapRequest(
		@NotNull UUID sessionId,
		@NotNull PaymentMethod paymentMethod,
		@NotEmpty @Size(max = 100) List<@Valid @NotNull SaleItemRequest> items) {
}
