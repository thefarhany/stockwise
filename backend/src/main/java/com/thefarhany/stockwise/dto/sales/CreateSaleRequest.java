package com.thefarhany.stockwise.dto.sales;

import com.thefarhany.stockwise.model.sales.PaymentMethod;
import com.thefarhany.stockwise.utils.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** cashReceived hanya untuk CASH (opsional); bila diisi harus >= total. Harga selalu dari server. */
public record CreateSaleRequest(
		@NotNull UUID sessionId,
		@NotNull PaymentMethod paymentMethod,
		@PositiveOrZero @Max(Money.MAX_AMOUNT) Long cashReceived,
		@NotEmpty @Size(max = 100) List<@Valid @NotNull SaleItemRequest> items) {
}
