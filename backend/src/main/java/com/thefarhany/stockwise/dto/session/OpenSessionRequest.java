package com.thefarhany.stockwise.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Seluruh body opsional: tanpa belanja awal, sesi dibuka dengan sisa kemarin saja. */
public record OpenSessionRequest(@Size(max = 100) List<@Valid @NotNull StockLineRequest> stock) {
}
