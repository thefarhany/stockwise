package com.thefarhany.stockwise.dto.catalog;

import com.thefarhany.stockwise.utils.Money;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RecipeItemRequest(
		@NotNull UUID stockItemId,
		@NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax(Money.MAX_RECIPE_QUANTITY) @Digits(integer = 9, fraction = 3) BigDecimal quantity) {
}
