package com.thefarhany.stockwise.dto.sales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidSaleRequest(@NotBlank @Size(max = 200) String reason) {
}
