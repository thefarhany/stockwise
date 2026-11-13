// Rentang tanggal untuk laporan tren dan ekspor Excel. Aturannya sama dengan backend utils/report/ReportRanges.

/** Paling panjang 366 hari, termasuk hari awal dan akhir. */
export const MAX_RANGE_DAYS = 366;
/** Tanpa rentang di URL: 30 hari terakhir. */
export const DEFAULT_RANGE_DAYS = 30;

export type TrendRange = { from: string; to: string; groupBy: "day" | "week" };

const DAY_MS = 86_400_000;
const toMs = (date: string) => Date.parse(`${date}T00:00:00Z`);
const fromMs = (ms: number) => new Date(ms).toISOString().slice(0, 10);

/** "YYYY-MM-DD" yang benar-benar ada di kalender (bukan 2026-02-31). */
const isDate = (v: unknown): v is string =>
  typeof v === "string" && /^\d{4}-\d{2}-\d{2}$/.test(v) && !Number.isNaN(toMs(v)) && fromMs(toMs(v)) === v;

/** `today` dikurangi `days` − 1 hari: awal rentang `days` hari yang berakhir hari ini. */
export const rangeStart = (to: string, days: number) => fromMs(toMs(to) - (days - 1) * DAY_MS);

/** Rentang dari query URL, diperbaiki supaya selalu lolos validasi backend: to ≤ hari ini, from ≤ to, ≤ 366 hari. */
export function trendRange(params: Record<string, string | string[] | undefined>, today: string): TrendRange {
  const to = isDate(params.to) && params.to <= today ? params.to : today;
  let from = isDate(params.from) && params.from <= to ? params.from : rangeStart(to, DEFAULT_RANGE_DAYS);
  if (from < rangeStart(to, MAX_RANGE_DAYS)) from = rangeStart(to, MAX_RANGE_DAYS);
  return { from, to, groupBy: params.groupBy === "week" ? "week" : "day" };
}
