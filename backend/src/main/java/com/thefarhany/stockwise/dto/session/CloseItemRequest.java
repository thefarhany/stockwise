package com.thefarhany.stockwise.dto.session;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/** Hasil hitung akhir satu bahan; carryOverQty + discardQty harus sama dengan closingQty. */
public record CloseItemRequest(
		@NotNull UUID stockItemId,
		@NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal closingQty,
		@NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal carryOverQty,
		@NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal discardQty) {
}
