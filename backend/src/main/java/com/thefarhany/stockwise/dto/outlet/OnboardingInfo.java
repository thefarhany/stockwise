package com.thefarhany.stockwise.dto.outlet;

import java.util.ArrayList;
import java.util.List;

/** Hitungan untuk panduan bertahap (FR-43). readyToOpen: profil lengkap + minimal satu bahan, produk, dan resep. */
public record OnboardingInfo(boolean profileDone, int stockItems, int products, int recipes, boolean readyToOpen) {

	/** Langkah yang masih kurang, untuk SETUP_REQUIRED. */
	public List<String> missing() {
		List<String> missing = new ArrayList<>();
		if (!profileDone) {
			missing.add("profile");
		}
		if (stockItems == 0) {
			missing.add("stockItems");
		}
		if (products == 0) {
			missing.add("products");
		}
		if (recipes == 0) {
			missing.add("recipes");
		}
		return missing;
	}

	public static OnboardingInfo of(boolean profileDone, OnboardingCounts c) {
		return new OnboardingInfo(profileDone, c.stockItems(), c.products(), c.recipes(),
				profileDone && c.stockItems() > 0 && c.products() > 0 && c.recipes() > 0);
	}

}
