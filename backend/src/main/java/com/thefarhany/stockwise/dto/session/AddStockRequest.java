package com.thefarhany.stockwise.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AddStockRequest(@NotEmpty @Size(max = 100) List<@Valid @NotNull StockLineRequest> items) {
}
