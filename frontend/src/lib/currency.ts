// Mata uang usaha (backend model/outlet/Currency). Semua uang dari API berupa bilangan bulat dalam satuan terkecil:
// rupiah untuk IDR, sen untuk yang lain (Rp15.000 = 15000, $15.00 = 1500).

export type Currency = "IDR" | "MYR" | "SGD" | "USD" | "EUR";

type CurrencySpec = {
  symbol: string;
  /** Angka di belakang koma. */
  decimals: number;
  /** Pemisah ribuan dan desimal mengikuti locale ini. */
  locale: string;
  /** Kelipatan saran harga jual, dalam satuan terkecil (sama dengan backend). */
  priceStep: number;
  /** Uang kertas besar yang biasa diterima, untuk tombol uang cepat di kasir. */
  cashNotes: number[];
};

export const currencies: Record<Currency, CurrencySpec> = {
  IDR: { symbol: "Rp", decimals: 0, locale: "id-ID", priceStep: 500, cashNotes: [50_000, 100_000] },
  MYR: { symbol: "RM", decimals: 2, locale: "en-US", priceStep: 50, cashNotes: [5_000, 10_000] },
  SGD: { symbol: "S$", decimals: 2, locale: "en-US", priceStep: 50, cashNotes: [1_000, 5_000] },
  USD: { symbol: "$", decimals: 2, locale: "en-US", priceStep: 50, cashNotes: [2_000, 5_000] },
  EUR: { symbol: "€", decimals: 2, locale: "en-US", priceStep: 50, cashNotes: [2_000, 5_000] },
};

export const currencyCodes = Object.keys(currencies) as Currency[];

const numberFormat = (c: Currency) =>
  new Intl.NumberFormat(currencies[c].locale, { minimumFractionDigits: currencies[c].decimals, maximumFractionDigits: currencies[c].decimals });

const decimalSeparator = (c: Currency) => (currencies[c].locale === "id-ID" ? "," : ".");

/** Satuan terkecil → angka tanpa simbol: 1500 → "15.00" (USD), 15000 → "15.000" (IDR). */
export const formatAmount = (amount: number, currency: Currency) =>
  numberFormat(currency).format(amount / 10 ** currencies[currency].decimals);

/** Satuan terkecil → "Rp15.000", "$15.00". */
export const formatMoney = (amount: number, currency: Currency) => `${currencies[currency].symbol}${formatAmount(amount, currency)}`;

/** Batas digit bagian utuh supaya angka tetap presisi. */
const MAX_WHOLE_DIGITS = 13;

/** Teks ketikan → bagian utuh dan bagian desimal (hanya digit); pemisah ribuan dan simbol diabaikan. */
function split(text: string, currency: Currency) {
  const { decimals } = currencies[currency];
  const sep = decimalSeparator(currency);
  const at = decimals > 0 ? text.indexOf(sep) : -1;
  const whole = (at < 0 ? text : text.slice(0, at)).replace(/\D/g, "").replace(/^0+(?=\d)/, "").slice(0, MAX_WHOLE_DIGITS - decimals);
  const fraction = at < 0 ? null : text.slice(at + 1).replace(/\D/g, "").slice(0, decimals);
  return { whole, fraction };
}

/** Teks ketikan → satuan terkecil: "15.5" → 1550 (USD), "15.000" → 15000 (IDR). Kosong → NaN. */
export function parseMoney(text: string, currency: Currency): number {
  const { whole, fraction } = split(text, currency);
  if (!whole && !fraction) return Number.NaN;
  const { decimals } = currencies[currency];
  return Number(whole || "0") * 10 ** decimals + Number((fraction ?? "").padEnd(decimals, "0") || "0");
}

/**
 * Isian uang diformat langsung selagi diketik: "14" → "14.00", "14000" → "14,000.00" (USD), "15000" → "15.000" (IDR).
 * Desimal selalu tampil penuh; kursor tetap di posisi digit yang sama, jadi angka yang diketik di bagian utuh tetap
 * masuk ke bagian utuh, dan "." memindahkan kursor ke sen (angka sen ditimpa, bukan disisipkan).
 * `caret` = posisi kursor di teks mentah; hasilnya posisi kursor di teks baru. Tanpa angka bukan nol → teks kosong.
 */
export function editMoney(raw: string, caret: number, currency: Currency): { text: string; caret: number } {
  const { decimals } = currencies[currency];
  const sep = decimalSeparator(currency);
  const at = decimals > 0 ? raw.indexOf(sep) : -1;
  const wholeRaw = at < 0 ? raw : raw.slice(0, at);
  const wholeDigits = wholeRaw.replace(/\D/g, "");
  const fractionDigits = at < 0 ? "" : raw.slice(at + 1).replace(/\D/g, "");
  // Bagian utuh dihapus semua dan sen masih nol (mis. backspace di "1.00") = isian dikosongkan.
  if (!wholeDigits && !/[1-9]/.test(fractionDigits)) return { text: "", caret: 0 };

  const whole = wholeDigits.replace(/^0+(?=\d)/, "").slice(0, MAX_WHOLE_DIGITS - decimals) || "0";
  const fraction = fractionDigits.slice(0, decimals).padEnd(decimals, "0");
  const grouped = new Intl.NumberFormat(currencies[currency].locale).format(Number(whole));
  const text = decimals > 0 ? `${grouped}${sep}${fraction}` : grouped;

  if (at >= 0 && caret > at) {
    // Kursor di bagian sen: sesudah n digit sen.
    const n = raw.slice(at + 1, caret).replace(/\D/g, "").length;
    return { text, caret: grouped.length + 1 + Math.min(n, decimals) };
  }
  // Kursor di bagian utuh: sesudah n digit dari kanan bagian utuh (nol di depan yang dibuang tidak mengubahnya).
  const fromRight = wholeRaw.slice(caret).replace(/\D/g, "").length;
  let pos = grouped.length;
  for (let seen = 0; pos > 0 && seen < fromRight; pos--) if (/\d/.test(grouped[pos - 1])) seen++;
  return { text, caret: pos };
}
