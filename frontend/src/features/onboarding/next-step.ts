import type { Onboarding } from "./types";

/** Halaman langkah onboarding pertama yang belum selesai, atau null bila usaha siap buka lapak. */
export function nextOnboardingStep(o: Onboarding): string | null {
  if (!o.profileDone) return "/onboarding";
  if (o.stockItems === 0) return "/onboarding/ingredients";
  // Produk dan resep dibuat dalam satu langkah.
  if (o.products === 0 || o.recipes === 0) return "/onboarding/products";
  return null;
}
