package com.thefarhany.stockwise.dto.catalog;

import java.util.List;
import java.util.UUID;

public record RecipeResponse(UUID productId, List<RecipeItemResponse> items) {
}
