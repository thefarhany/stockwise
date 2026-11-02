package com.thefarhany.stockwise.dto.outlet;

import jakarta.validation.constraints.NotBlank;

public record DeleteOutletRequest(@NotBlank String confirmName) {
}
