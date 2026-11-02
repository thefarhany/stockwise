package com.thefarhany.stockwise.dto.outlet;


/** Hitungan data aktif milik satu usaha; recipes = produk aktif yang punya minimal satu baris resep. */
public record OnboardingCounts(int stockItems, int products, int recipes) {
}
