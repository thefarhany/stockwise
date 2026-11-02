package com.thefarhany.stockwise.model.catalog;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record RecipeItemId(UUID productId, UUID stockItemId) implements Serializable {
}
