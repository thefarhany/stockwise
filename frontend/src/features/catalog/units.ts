import type { StockUnit } from "./types";

export type UnitGroup = "weight" | "volume" | "count" | "package";

/**
 * Satuan bahan, sama dengan backend StockUnit. factor = kelipatan satuan dasar sekeluarga (gram, ml, pcs); satuan
 * kemasan jadi keluarga sendiri karena isinya beda per barang. `id`/`en` = label singkat per bahasa; nama panjang dan
 * nama kelompok ada di messages (units.names, units.groups). Urutan = urutan di form.
 */
export const stockUnits: Record<StockUnit, { group: UnitGroup; family: string; factor: number; id: string; en: string }> = {
  GRAM: { group: "weight", family: "mass", factor: 1, id: "g", en: "g" },
  KG: { group: "weight", family: "mass", factor: 1_000, id: "kg", en: "kg" },
  ML: { group: "volume", family: "volume", factor: 1, id: "ml", en: "ml" },
  LITER: { group: "volume", family: "volume", factor: 1_000, id: "liter", en: "liter" },
  PCS: { group: "count", family: "count", factor: 1, id: "pcs", en: "pcs" },
  TRAY: { group: "count", family: "count", factor: 30, id: "tray", en: "tray" },
  BOTOL: { group: "package", family: "BOTOL", factor: 1, id: "botol", en: "bottle" },
  PACK: { group: "package", family: "PACK", factor: 1, id: "pack", en: "pack" },
  DUS: { group: "package", family: "DUS", factor: 1, id: "dus", en: "box" },
  RENCENG: { group: "package", family: "RENCENG", factor: 1, id: "renceng", en: "strip" },
};

export const stockUnitKeys = Object.keys(stockUnits) as [StockUnit, ...StockUnit[]];

/** "kg", "botol" (id) / "bottle" (en). */
export const unitLabel = (unit: StockUnit, locale: string) => stockUnits[unit][locale === "id" ? "id" : "en"];

const sameFamily = (a: StockUnit, b: StockUnit) => stockUnits[a].family === stockUnits[b].family;

/**
 * Satuan yang boleh dipakai resep untuk bahan bersatuan `stock`: sekeluarga, dan tidak lebih halus dari 0,001 satuan
 * stok (backend menyimpan jumlah resep maksimal 3 desimal). Contoh: kg → gram dan kg.
 */
export const quantityUnitsFor = (stock: StockUnit): StockUnit[] =>
  stockUnitKeys.filter((u) => sameFamily(u, stock) && stockUnits[u].factor * 1000 >= stockUnits[stock].factor);

/** Satuan kecil sekeluarga, dipakai resep dan tampilan di bawah 1 satuan besar: kg → gram, liter → ml, tray → pcs. */
export const defaultQuantityUnit = (stock: StockUnit): StockUnit =>
  stockUnitKeys.find((u) => sameFamily(u, stock) && stockUnits[u].factor === 1) ?? stock;

/** Jumlah resep dalam satuan stok, atau null bila hasilnya lebih dari 3 desimal. */
export function toStockQuantity(quantity: number, unit: StockUnit, stock: StockUnit): number | null {
  const milli = (Math.round(quantity * 1000) * stockUnits[unit].factor) / stockUnits[stock].factor;
  return Number.isInteger(milli) ? milli / 1000 : null;
}

/** Kebalikan toStockQuantity, untuk menampilkan resep tersimpan. */
export const fromStockQuantity = (quantity: number, unit: StockUnit, stock: StockUnit) =>
  (Math.round(quantity * 1000) * stockUnits[stock].factor) / stockUnits[unit].factor / 1000;

/** Satuan beli yang dikonversi backend ke satuan bahan (UnitConverter): sekeluarga. Contoh: pcs → pcs, tray. */
export const purchaseUnitsFor = (stock: StockUnit): StockUnit[] => stockUnitKeys.filter((u) => sameFamily(u, stock));

/** Jumlah dalam satuan lain sekeluarga, tanpa pembulatan: 2 KG → 2000 GRAM. */
export const convertQuantity = (qty: number, from: StockUnit, to: StockUnit) =>
  (qty * stockUnits[from].factor) / stockUnits[to].factor;

const show = (qty: number, unit: StockUnit, locale: string) =>
  `${qty.toLocaleString(locale, { maximumFractionDigits: 3 })} ${unitLabel(unit, locale)}`;

/**
 * Format jumlah yang dipakai di semua layar: di bawah 1 satuan besar pakai satuan kecil supaya tidak ada nol koma
 * (0.9 KG → "900 g", 0.5 TRAY → "15 pcs"), selain itu satuan stok (7.2 KG → "7.2 kg" / "7,2 kg").
 */
export const formatQuantity = (qty: number, unit: StockUnit, locale: string) =>
  qty > 0 && qty < 1 && defaultQuantityUnit(unit) !== unit ? formatSmallQuantity(qty, unit, locale) : show(qty, unit, locale);

/** Selalu dalam satuan kecil sekeluarga, untuk tabel yang dibandingkan per baris: 1.7 LITER → "1,700 ml". */
export function formatSmallQuantity(qty: number, unit: StockUnit, locale: string) {
  const small = defaultQuantityUnit(unit);
  return show(fromStockQuantity(qty, small, unit), small, locale);
}
