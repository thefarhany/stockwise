import type { StockUnit } from "./types";

export type UnitGroup = "weight" | "volume" | "count" | "package";

/**
 * Label satuan: `short` untuk daftar (sama di semua bahasa), `group` untuk mengelompokkan pilihan. Nama panjang dan nama
 * kelompok ada di messages (units.names, units.groups). Urutan = urutan di form.
 */
export const stockUnits: Record<StockUnit, { short: string; group: UnitGroup }> = {
  GRAM: { short: "gram", group: "weight" },
  ONS: { short: "ons", group: "weight" },
  KG: { short: "kg", group: "weight" },
  ML: { short: "ml", group: "volume" },
  SDT: { short: "sdt", group: "volume" },
  SDM: { short: "sdm", group: "volume" },
  CUP: { short: "cup", group: "volume" },
  LITER: { short: "liter", group: "volume" },
  PCS: { short: "pcs", group: "count" },
  BUTIR: { short: "butir", group: "count" },
  BUAH: { short: "buah", group: "count" },
  LUSIN: { short: "lusin", group: "count" },
  KODI: { short: "kodi", group: "count" },
  TRAY: { short: "tray", group: "count" },
  SIUNG: { short: "siung", group: "count" },
  IKAT: { short: "ikat", group: "count" },
  BATANG: { short: "batang", group: "count" },
  LEMBAR: { short: "lembar", group: "count" },
  EKOR: { short: "ekor", group: "count" },
  POTONG: { short: "potong", group: "count" },
  SISIR: { short: "sisir", group: "count" },
  PORSI: { short: "porsi", group: "count" },
  BUNGKUS: { short: "bungkus", group: "package" },
  PACK: { short: "pack", group: "package" },
  BOTOL: { short: "botol", group: "package" },
  KALENG: { short: "kaleng", group: "package" },
  GALON: { short: "galon", group: "package" },
  JERIGEN: { short: "jerigen", group: "package" },
};

export const stockUnitKeys = Object.keys(stockUnits) as [StockUnit, ...StockUnit[]];

/** Satuan untuk jumlah di resep: satuan stok, plus miligram yang tidak ada di backend. */
export type QuantityUnit = StockUnit | "MG";

// factor = kelipatan satuan terkecil sekeluarga (mg untuk berat, ml untuk volume, pcs untuk hitungan); bilangan bulat
// supaya hitungan pasti. Sama dengan backend StockUnit. Satuan tanpa isi tetap jadi keluarga sendiri.
const alone = (unit: StockUnit) => ({ family: unit, factor: 1 });
const quantityUnits: Record<QuantityUnit, { family: string; factor: number }> = {
  MG: { family: "mass", factor: 1 },
  GRAM: { family: "mass", factor: 1_000 },
  ONS: { family: "mass", factor: 100_000 },
  KG: { family: "mass", factor: 1_000_000 },
  ML: { family: "volume", factor: 1 },
  SDT: { family: "volume", factor: 5 },
  SDM: { family: "volume", factor: 15 },
  CUP: { family: "volume", factor: 240 },
  LITER: { family: "volume", factor: 1_000 },
  PCS: { family: "count", factor: 1 },
  BUTIR: { family: "count", factor: 1 },
  BUAH: { family: "count", factor: 1 },
  LUSIN: { family: "count", factor: 12 },
  KODI: { family: "count", factor: 20 },
  TRAY: { family: "count", factor: 30 },
  SIUNG: alone("SIUNG"),
  IKAT: alone("IKAT"),
  BATANG: alone("BATANG"),
  LEMBAR: alone("LEMBAR"),
  EKOR: alone("EKOR"),
  POTONG: alone("POTONG"),
  SISIR: alone("SISIR"),
  PORSI: alone("PORSI"),
  BUNGKUS: alone("BUNGKUS"),
  PACK: alone("PACK"),
  BOTOL: alone("BOTOL"),
  KALENG: alone("KALENG"),
  GALON: alone("GALON"),
  JERIGEN: alone("JERIGEN"),
};

export const quantityUnitKeys = Object.keys(quantityUnits) as [QuantityUnit, ...QuantityUnit[]];

export const quantityUnitLabel = (unit: QuantityUnit) => (unit === "MG" ? "mg" : stockUnits[unit].short);

/**
 * Satuan yang boleh dipakai resep untuk bahan bersatuan `stock`: sekeluarga, dan tidak lebih halus dari
 * 0,001 satuan stok (backend menyimpan jumlah resep maksimal 3 desimal). Contoh: kg → gram dan kg, tanpa mg.
 */
export function quantityUnitsFor(stock: StockUnit): QuantityUnit[] {
  const s = quantityUnits[stock];
  return (Object.keys(quantityUnits) as QuantityUnit[]).filter(
    (u) => quantityUnits[u].family === s.family && quantityUnits[u].factor * 1000 >= s.factor,
  );
}

/** Satuan resep bawaan: satuan kecil sekeluarga, karena resep dihitung per porsi (kg/ons → gram, liter → ml). */
export const defaultQuantityUnit = (stock: StockUnit): QuantityUnit =>
  stock === "KG" || stock === "ONS" ? "GRAM" : stock === "LITER" ? "ML" : stock;

/** Jumlah resep dalam satuan stok, atau null bila hasilnya lebih dari 3 desimal. */
export function toStockQuantity(quantity: number, unit: QuantityUnit, stock: StockUnit): number | null {
  const milli = (Math.round(quantity * 1000) * quantityUnits[unit].factor) / quantityUnits[stock].factor;
  return Number.isInteger(milli) ? milli / 1000 : null;
}

/** Kebalikan toStockQuantity, untuk menampilkan resep tersimpan. */
export const fromStockQuantity = (quantity: number, unit: QuantityUnit, stock: StockUnit) =>
  (Math.round(quantity * 1000) * quantityUnits[stock].factor) / quantityUnits[unit].factor / 1000;

/** Satuan beli yang dikonversi backend ke satuan bahan (UnitConverter): sekeluarga, tanpa mg. Contoh: gram → gram, ons, kg. */
export const purchaseUnitsFor = (stock: StockUnit): StockUnit[] =>
  stockUnitKeys.filter((u) => quantityUnits[u].family === quantityUnits[stock].family);

/** Jumlah dalam satuan lain sekeluarga, tanpa pembulatan: 2 KG → 2000 GRAM. */
export const convertQuantity = (qty: number, from: StockUnit, to: StockUnit) =>
  (qty * quantityUnits[from].factor) / quantityUnits[to].factor;

/** 7.2 + KG → "7.2 kg" (en) / "7,2 kg" (id). */
export const formatQuantity = (qty: number, unit: StockUnit, locale: string) =>
  `${qty.toLocaleString(locale, { maximumFractionDigits: 3 })} ${stockUnits[unit].short}`;

const smallLabel: Partial<Record<QuantityUnit, string>> = { GRAM: "g", ML: "ml" };

/** Jumlah dalam satuan kecil sekeluarga, supaya tidak ada nol koma: 0.54 KG → "540 g", 1.7 LITER → "1,700 ml". */
export function formatSmallQuantity(qty: number, unit: StockUnit, locale: string) {
  const small = defaultQuantityUnit(unit);
  const value = fromStockQuantity(qty, small, unit);
  return `${value.toLocaleString(locale, { maximumFractionDigits: 3 })} ${smallLabel[small] ?? quantityUnitLabel(small)}`;
}
