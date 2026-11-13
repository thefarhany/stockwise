import Link from "next/link";
import { useTranslations } from "next-intl";
import { LuArrowRight } from "react-icons/lu";
import { InsightPanel } from "@/features/reports/components/insight-panel";
import type { SessionSummary } from "@/features/reports/types";
import type { SessionStock } from "@/features/session/types";
import { useFormat } from "@/i18n/format";
import { formatMoney, type Currency } from "@/lib/currency";
import { formatClock } from "@/lib/format";

/** Ringkasan sesi tertutup: laba, rincian, barang utama, dan insight AI (buat/buat ulang). */
export function SummaryCard({
  currency,
  summary,
  closedAt,
  timeZone,
  title,
  primaryStock,
}: {
  /** Mata uang usaha. */
  currency: Currency;
  summary: SessionSummary;
  closedAt: string | null;
  timeZone: string;
  title: string;
  /** Baris stok barang utama di sesi itu, untuk "tidak habis, sisa dibawa". */
  primaryStock?: SessionStock;
}) {
  const t = useTranslations("summary");
  const f = useFormat();
  const money = (n: number) => formatMoney(n, currency);
  const s = summary;
  const rows = [
    { label: t("revenue"), value: money(s.revenue) },
    { label: t("cogs"), value: money(s.cogs) },
    { label: t("wasted"), value: money(s.wasteCost) },
    ...(s.adjustmentCost !== 0 ? [{ label: t("adjustments"), value: money(s.adjustmentCost) }] : []),
    { label: t("carried"), value: money(s.carriedValue) },
    { label: t("sold"), value: t("portions", { count: s.totalItemsSold }) },
    { label: t("voided"), value: String(s.voidCount) },
  ];
  const closed = closedAt
    ? t(s.closeType === "AUTO" ? "closedAutoAt" : "closedAt", { time: formatClock(closedAt, timeZone) })
    : t("closed");
  const p = s.primaryItem;
  const missed = Math.round(p?.lostSalesEstimate ?? 0);

  return (
    <section aria-labelledby="summary-heading" className="overflow-hidden rounded-box border border-base-300 bg-white/60">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-base-300 px-6 py-5">
        <div>
          <h2 id="summary-heading" className="text-xl font-semibold">{title}</h2>
          <p className="mt-1 text-sm text-base-content/70">
            {f.date(s.businessDate)} · {closed}
          </p>
        </div>
        <Link href={`/reports?date=${s.businessDate}`} className="flex items-center gap-2 text-sm hover:underline">
          {t("dailyReport")} <LuArrowRight className="size-4" aria-hidden />
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-6 px-6 py-6 md:grid-cols-[1fr_1.6fr]">
        <div>
          <p className="text-3xl font-semibold tracking-tight">{money(s.netProfit)}</p>
          <p className="mt-1 text-sm text-base-content/70">{t("profitBefore")}</p>
          {p?.soldOutAt ? (
            <div className="mt-4 rounded-box bg-base-200 px-4 py-3 text-sm">
              <p className="font-semibold">
                {t("soldOutAt", { name: p.name, time: formatClock(p.soldOutAt, timeZone) })}
              </p>
              {missed > 0 && <p className="mt-1 text-xs text-base-content/70">{t("missed", { count: missed })}</p>}
            </div>
          ) : (
            p &&
            primaryStock && (
              <div className="mt-4 rounded-box bg-base-200 px-4 py-3 text-sm">
                <p className="font-semibold">{t("didntSellOut", { name: p.name })}</p>
                <p className="mt-1 text-xs text-base-content/70">
                  {(primaryStock.carriedOverQty ?? 0) > 0
                    ? t("leftCarried", { qty: f.quantity(primaryStock.carriedOverQty ?? 0, primaryStock.unit) })
                    : t("leftAtClose", { qty: f.quantity(primaryStock.closingQty ?? 0, primaryStock.unit) })}
                </p>
              </div>
            )
          )}
        </div>
        <dl className="divide-y divide-base-300 text-sm">
          {rows.map((r) => (
            <div key={r.label} className="flex justify-between gap-4 py-2.5">
              <dt>{r.label}</dt>
              <dd>{r.value}</dd>
            </div>
          ))}
        </dl>
      </div>

      <InsightPanel key={s.sessionId} sessionId={s.sessionId} initial={s.insight} timeZone={timeZone} />
    </section>
  );
}
