// Pemformat yang mengikuti bahasa tampilan: useFormat() di komponen (client atau server non-async); getFormat() untuk
// server component async ada di format-server.ts.
import { useLocale } from "next-intl";
import { formatQuantity, formatSmallQuantity, unitLabel } from "@/features/catalog/units";
import type { StockUnit } from "@/features/catalog/types";
import { formatBusinessDate, formatWeekday } from "@/lib/format";

export function bindFormat(locale: string) {
  return {
    /** "Thursday, 5 November" */
    date: (date: string) => formatBusinessDate(date, locale),
    /** "Thursday" */
    weekday: (date: string) => formatWeekday(date, locale),
    /** "kg", "botol" / "bottle" */
    unit: (unit: StockUnit) => unitLabel(unit, locale),
    /** "7.2 kg", "900 g" */
    quantity: (qty: number, unit: StockUnit) => formatQuantity(qty, unit, locale),
    /** "540 g" */
    smallQuantity: (qty: number, unit: StockUnit) => formatSmallQuantity(qty, unit, locale),
    /** Angka biasa: 1,234 (en) / 1.234 (id). */
    number: (n: number, options?: Intl.NumberFormatOptions) => n.toLocaleString(locale, options),
  };
}

export type Format = ReturnType<typeof bindFormat>;

export function useFormat(): Format {
  return bindFormat(useLocale());
}
