import { useTranslations } from "next-intl";
import { parseStockout } from "@/features/session/components/recommendation-card";
import type { Recommendation, SessionStock } from "@/features/session/types";
import type { Format } from "@/i18n/format";
import { formatMoney, type Currency } from "@/lib/currency";
import { formatClock } from "@/lib/format";

/** chip.key = kunci di messages leftovers.*; time untuk "Sold out {time}". */
type Row = { id: string; name: string; detail: string; chip: { dot: string; key: "carried" | "discarded" | "soldOutAt" | "soldOut"; time?: string } };

const carried = { dot: "bg-success", key: "carried" } as const;

/** Baris dari sesi tertutup (Owner): yang dibawa, yang dibuang, dan yang habis. */
export function leftoverRowsFromSession(stock: SessionStock[], timeZone: string, currency: Currency, f: Format): Row[] {
  return stock
    .filter((s) => (s.carriedOverQty ?? 0) > 0 || (s.discardedQty ?? 0) > 0 || s.soldOutAt)
    .map((s) => {
      const discarded = (s.discardedQty ?? 0) > 0;
      const chip: Row["chip"] =
        s.soldOutAt && !discarded && !(s.carriedOverQty ?? 0)
          ? { dot: "bg-error", key: "soldOutAt", time: formatClock(s.soldOutAt, timeZone) }
          : discarded
            ? { dot: "bg-error", key: "discarded" }
            : carried;
      const detail = discarded
        ? `${f.quantity(s.discardedQty ?? 0, s.unit)}${s.discardedCost !== null ? ` · ${formatMoney(s.discardedCost, currency)}` : ""}`
        : f.quantity(s.carriedOverQty ?? 0, s.unit);
      return { id: s.stockItemId, name: s.name, detail, chip };
    });
}

/** Baris dari saran belanja (Cashier tidak bisa membaca sesi tertutup): sisa yang dibawa dan bahan yang habis. */
export function leftoverRowsFromRecommendation(rec: Recommendation, f: Format): Row[] {
  return rec.items.flatMap((i): Row[] => {
    const stockout = i.reasons.find((r) => r.code === "STOCKOUT_CORRECTION");
    if (i.carryOverQty > 0) return [{ id: i.stockItemId, name: i.name, detail: f.quantity(i.carryOverQty, i.unit), chip: carried }];
    if (!stockout) return [];
    const at = parseStockout(stockout.text)?.time;
    return [{ id: i.stockItemId, name: i.name, detail: f.quantity(0, i.unit), chip: at ? { dot: "bg-error", key: "soldOutAt", time: at } : { dot: "bg-error", key: "soldOut" } }];
  });
}

/** Kartu sisa: kosong = tidak ditampilkan. */
export function LeftoversCard({ rows, title, lead }: { rows: Row[]; title: string; lead: string }) {
  const t = useTranslations("leftovers");
  if (rows.length === 0) return null;
  return (
    <section aria-labelledby="leftovers-heading" className="rounded-box border border-base-300 bg-white/60 p-6">
      <h2 id="leftovers-heading" className="text-xl font-semibold">{title}</h2>
      <p className="mt-1 text-sm text-base-content/70">{lead}</p>
      <ul className="mt-4 divide-y divide-base-300 border-t border-base-300 text-sm">
        {rows.map((r) => (
          <li key={r.id} className="flex items-center justify-between gap-4 py-3">
            <span className="min-w-0">
              <span className="block truncate">{r.name}</span>
              <span className="text-xs text-base-content/70">{r.detail}</span>
            </span>
            <span className="flex shrink-0 items-center gap-2 rounded-full bg-base-200 px-2.5 py-0.5 text-xs">
              <span aria-hidden className={`size-1.5 rounded-full ${r.chip.dot}`} />
              {t(r.chip.key, { time: r.chip.time ?? "" })}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
