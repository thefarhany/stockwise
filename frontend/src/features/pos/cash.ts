import { currencies, type Currency } from "@/lib/currency";

/** Uang kertas yang biasa diterima di atas total: kelipatan berikutnya dari pecahan besar (Rp50.000 dan Rp100.000 untuk rupiah). */
export function quickCashOptions(total: number, currency: Currency): number[] {
  if (total <= 0) return [];
  // Kelipatan pecahan yang lebih besar dari total; total yang pas sudah diwakili tombol "uang pas".
  return [...new Set(currencies[currency].cashNotes.map((note) => (Math.floor(total / note) + 1) * note))];
}
