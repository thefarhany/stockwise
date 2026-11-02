package com.thefarhany.stockwise.dto.sales;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SaleItemRequest(@NotNull UUID productId, @Min(1) @Max(100000) int quantity) {
}
