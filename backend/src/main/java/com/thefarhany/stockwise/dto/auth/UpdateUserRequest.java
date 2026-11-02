package com.thefarhany.stockwise.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Field yang null tidak diubah. */
public record UpdateUserRequest(
		@Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "tidak boleh kosong") String name,
		Boolean active) {
}
