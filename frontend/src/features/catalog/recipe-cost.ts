import { currencies, type Currency } from "@/lib/currency";

// Pratinjau HPP dan saran harga di browser; rumusnya sama dengan backend (ProductResponse, PriceSuggester).
// Hitungan memakai bilangan bulat supaya pembulatannya sama persis.

/** Biaya satu baris resep dalam satuan terkecil mata uang (belum dibulatkan ke rupiah utuh untuk total). */
const milliCost = (quantity: number, unitCost: number) => Math.round(quantity * 1000) * unitCost;

/** Satuan terkecil utuh (rupiah, sen), dibulatkan HALF_UP; jumlah resep paling banyak 3 desimal. */
export const lineCost = (quantity: number, unitCost: number) => Math.round(milliCost(quantity, unitCost) / 1000);

/** HPP per porsi: jumlah semua baris, baru dibulatkan. */
export const recipeCost = (lines: { quantity: number; unitCost: number }[]) =>
  Math.round(lines.reduce((sum, l) => sum + milliCost(l.quantity, l.unitCost), 0) / 1000);

/** harga = HPP / (1 − margin), naik ke kelipatan harga mata uang (Rp500, atau 0.50). null bila HPP nol. */
export function suggestPrice(hpp: number, targetMarginPct: number, currency: Currency): number | null {
  if (hpp <= 0) return null;
  const marginBasisPoints = Math.round(targetMarginPct * 100);
  const raw = Math.ceil((hpp * 10_000) / (10_000 - marginBasisPoints));
  const step = currencies[currency].priceStep;
  return Math.ceil(raw / step) * step;
}
