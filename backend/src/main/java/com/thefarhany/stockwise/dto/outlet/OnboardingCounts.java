package com.thefarhany.stockwise.dto.outlet;


/**
 * Hitungan data aktif milik satu usaha; recipes = produk aktif yang punya minimal satu baris resep.
 * hasSessions = lapak sudah pernah dibuka (sudah ada riwayat uang).
 */
public record OnboardingCounts(int stockItems, int products, int recipes, boolean hasSessions) {
}
