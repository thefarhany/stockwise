package com.thefarhany.stockwise.dto.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Mengganti seluruh resep; daftar kosong = hapus resep. */
public record RecipeRequest(@NotNull @Size(max = 50) List<@Valid @NotNull RecipeItemRequest> items) {
}
