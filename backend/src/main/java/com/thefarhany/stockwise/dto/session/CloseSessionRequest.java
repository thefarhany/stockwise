package com.thefarhany.stockwise.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CloseSessionRequest(
		@NotNull @Size(max = 100) List<@Valid @NotNull CloseItemRequest> items,
		@Size(max = 1000) String notes) {
}
