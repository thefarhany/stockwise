import { useTranslations } from "next-intl";
import type { SessionSummary } from "@/features/reports/types";
import type { Recommendation, RecommendationItem } from "@/features/session/types";
import { useFormat, type Format } from "@/i18n/format";
import { formatMoney, type Currency } from "@/lib/currency";
import { formatClock } from "@/lib/format";

export type RecommendationT = ReturnType<typeof useTranslations<"recommendation">>;

type SoldOut = { primary: SessionSummary["primaryItem"]; timeZone: string };

/**
 * Jam habis dan pembeli terlewat dari teks alasan backend ("Kemarin habis jam 17.20, sekitar 9 pembeli terlewat").
 * Dipakai bila ringkasan sesi tidak bisa dibaca (Cashier). null bila teksnya tidak berpola itu.
 */
export function parseStockout(text: string): { time: string; missed: number } | null {
  const time = /jam (\d{1,2})[.:](\d{2})/.exec(text);
  if (!time) return null;
  const missed = /sekitar (\d+) pembeli/.exec(text);
  return { time: `${time[1]!.padStart(2, "0")}:${time[2]}`, missed: missed ? Number(missed[1]) : 0 };
}

/** Alasan dalam bahasa tampilan dari `code`; backend mengirim `text` berbahasa Indonesia. Kode tak dikenal memakai `text`. */
export function reasonText(
  code: string,
  text: string,
  item: RecommendationItem,
  rec: Recommendation,
  soldOut: SoldOut,
  t: RecommendationT,
  f: Format,
) {
  const day = f.weekday(rec.targetDate);
  switch (code) {
    case "WEEKDAY_UPLIFT":
      return t("weekdayUp", { day });
    case "WEEKDAY_LOWER":
      return t("weekdayDown", { day });
    case "STOCKOUT_CORRECTION": {
      const p = soldOut.primary;
      const fromSummary =
        p?.stockItemId === item.stockItemId && p.soldOutAt
          ? { time: formatClock(p.soldOutAt, soldOut.timeZone), missed: Math.round(p.lostSalesEstimate ?? 0) }
          : null;
      const found = fromSummary ?? parseStockout(text);
      if (!found) return t("soldOutEarly", { name: item.name });
      return found.missed > 0
        ? t("soldOutMissed", { name: item.name, time: found.time, missed: found.missed })
        : t("soldOutAt", { name: item.name, time: found.time });
    }
    case "CARRY_OVER":
      return t("carryOver");
    case "LOW_DATA":
      return t("lowData", { count: rec.dataPoints });
    default:
      return text;
  }
}

const heading = "text-xs uppercase tracking-widest text-neutral-content/70";

/** Kartu gelap saran belanja. `rec` null = belum ada saran untuk tanggal itu. */
export function RecommendationCard({
  currency,
  rec,
  title,
  primaryIds,
  soldOut,
}: {
  /** Mata uang usaha. */
  currency: Currency;
  rec: Recommendation | null;
  title: string;
  primaryIds: Set<string>;
  /** Barang utama dari ringkasan terakhir, untuk jam habis dan pembeli terlewat di alasan. */
  soldOut: SoldOut;
}) {
  const t = useTranslations("recommendation");
  const f = useFormat();
  const money = (n: number) => formatMoney(n, currency);
  if (!rec || rec.items.length === 0) {
    return (
      <section aria-labelledby="rec-heading" className="rounded-box bg-neutral p-6 text-neutral-content">
        <h2 id="rec-heading" className={heading}>{title}</h2>
        <p className="mt-3 text-xl font-semibold">{t("noneTitle")}</p>
        <p className="mt-2 text-sm text-neutral-content/70">
          {t("noneText")}
        </p>
      </section>
    );
  }

  const total = rec.items.reduce((sum, i) => sum + (i.estimatedCost ?? 0), 0);
  // Cashier tidak melihat biaya (estimatedCost null): tampilkan jumlah bahan, bukan uang.
  const showCost = rec.items.some((i) => i.estimatedCost !== null);
  const n = rec.items.length;
  const reasons = [
    ...new Set(rec.items.flatMap((i) => i.reasons.map((r) => reasonText(r.code, r.text, i, rec, soldOut, t, f)))),
  ];

  return (
    <section aria-labelledby="rec-heading" className="relative rounded-box bg-neutral p-6 text-neutral-content">
      <h2 id="rec-heading" className={heading}>{title}</h2>
      <p className="mt-2 text-3xl font-semibold tracking-tight">
        {showCost ? money(total) : t("toPrepare", { count: n })}
      </p>
      <p className="mt-2 text-sm text-neutral-content/70">
        {t(showCost ? "costFor" : "guideFor", { day: f.weekday(rec.targetDate) })}
        {rec.dataPoints > 0 ? t("fromData", { count: rec.dataPoints }) : t("startingEstimate")}
        {!showCost && rec.items.some((i) => i.carryOverQty > 0) && t("leftoversSubtracted")}
      </p>

      <details className="group mt-4">
        <summary className="btn btn-sm absolute top-5 right-5 rounded-full border-neutral-content/60 bg-transparent font-normal text-neutral-content hover:bg-neutral-content/10">
          {t("why")}
        </summary>
        <ul className="space-y-1 rounded-field bg-neutral-content/10 p-4 text-sm">
          {rec.items.map((i) => (
            <li key={i.stockItemId}>
              {t("needs", { name: i.name, required: f.quantity(i.requiredQty, i.unit) })}
              {i.carryOverQty > 0 && t("minusLeft", { left: f.quantity(i.carryOverQty, i.unit) })}
              {t("soBuy", { buy: f.quantity(i.recommendedQty, i.unit) })}
            </li>
          ))}
        </ul>
      </details>

      <div className="relative mt-5 overflow-x-auto">
        <table className="w-full min-w-xl text-left text-sm">
          <thead className="text-xs uppercase tracking-widest text-neutral-content/70">
            <tr className="border-b border-neutral-content/30">
              <th className="py-3 font-normal">{t("colIngredient")}</th>
              <th className="py-3 text-center font-normal">{t("colAmount")}</th>
              <th className="py-3 text-center font-normal">{t("colRange")}</th>
              {showCost && <th className="py-3 text-center font-normal">{t("colCost")}</th>}
            </tr>
          </thead>
          <tbody>
            {rec.items.map((i) => (
              <tr key={i.stockItemId} className="border-b border-neutral-content/30">
                <td className="py-3">
                  {i.name}
                  {primaryIds.has(i.stockItemId) && (
                    <span className="ml-2 rounded-full bg-neutral-content px-2 py-0.5 text-xs text-neutral">{t("mainItem")}</span>
                  )}
                </td>
                <td className="py-3 text-center">{f.quantity(i.recommendedQty, i.unit)}</td>
                <td className="py-3 text-center text-xs text-neutral-content/70">
                  {f.quantity(i.lowerQty, i.unit)} – {f.quantity(i.upperQty, i.unit)}
                </td>
                {showCost && <td className="py-3 text-center">{i.estimatedCost === null ? "–" : money(i.estimatedCost)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {reasons.length > 0 && (
        <div className="mt-6">
          <h3 className="text-sm font-semibold">{t("reasons")}</h3>
          <ul className="mt-2 flex flex-wrap gap-2">
            {reasons.map((r) => (
              <li key={r} className="rounded-full border border-neutral-content/60 px-3 py-1 text-xs">{r}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
