import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import type { PaymentMethod } from "@/features/pos/types";
import { busiestHours, hourlySeries } from "@/features/reports/hourly";
import type { DailyReport, SessionSummary } from "@/features/reports/types";
import type { RecommendationItem, SessionStock } from "@/features/session/types";
import { useFormat } from "@/i18n/format";
import { formatMoney, type Currency } from "@/lib/currency";
import { formatClock } from "@/lib/format";

const card = "rounded-box border border-base-300 bg-white/60";
const pct = (part: number, whole: number) => (whole > 0 ? Math.round((part / whole) * 100) : 0);
const hh = (hour: number) => String(hour).padStart(2, "0");

/** Batang tipis horizontal untuk bagian dari total (metode bayar, porsi produk). */
function Meter({ value, label }: { value: number; label: string }) {
  return (
    <span className="block h-1.5 w-full overflow-hidden rounded-full bg-base-200" role="img" aria-label={label}>
      <span className="block h-full rounded-full bg-secondary" style={{ width: `${Math.min(value, 100)}%` }} />
    </span>
  );
}

function CardHeading({ title, lead }: { title: string; lead?: string }) {
  return (
    <>
      <h2 className="text-xl font-semibold">{title}</h2>
      {lead && <p className="mt-1 text-sm text-base-content/70">{lead}</p>}
    </>
  );
}

/** Kartu gelap laba: rincian omzet dikurangi biaya. */
export function ProfitCard({ report, portions, currency }: { report: DailyReport; portions: number; currency: Currency }) {
  const t = useTranslations("report");
  const money = (n: number) => formatMoney(n, currency);
  const rows: { sign: string; label: string; value: number; bold?: true }[] = [
    { sign: "", label: t("revenue"), value: report.revenue },
    { sign: "–", label: t("cogs"), value: report.cogs },
    { sign: "–", label: t("wasted"), value: report.wasteCost },
    { sign: "–", label: t("differences"), value: report.adjustmentCost },
    ...(report.expenseTotal !== 0 ? [{ sign: "–", label: t("operating"), value: report.expenseTotal }] : []),
    { sign: "=", label: t("profit"), value: report.netProfit, bold: true },
  ];
  return (
    <section aria-labelledby="profit-heading" className="rounded-box bg-neutral p-7 text-neutral-content">
      <h2 id="profit-heading" className="text-xs uppercase tracking-widest text-neutral-content/70">
        {t("profitBefore")}
      </h2>
      <p className="mt-2 text-4xl font-semibold tracking-tight">{money(report.netProfit)}</p>
      <p className="mt-2 text-sm text-neutral-content/70">
        {t("fromPortions", { portions, transactions: report.transactions })}
      </p>
      <dl className="mt-5 divide-y divide-neutral-content/20 border-t border-neutral-content/20 text-sm">
        {rows.map((r) => (
          <div key={r.label} className={`flex justify-between gap-4 py-3 ${r.bold ? "font-semibold" : ""}`}>
            <dt>
              <span aria-hidden className="inline-block w-6 text-neutral-content/60">{r.sign}</span>
              {r.label}
            </dt>
            <dd>{money(r.value)}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

/** Catatan hari itu: item utama, sisa dibawa, transaksi batal, saran untuk besok. */
export function NotesCard({
  currency,
  summary,
  stock,
  voids,
  nextRec,
  nextDate,
  timeZone,
}: {
  /** Mata uang usaha. */
  currency: Currency;
  summary: SessionSummary | null;
  stock: SessionStock[];
  voids: { receiptNo: number; reason: string | null }[];
  nextRec: RecommendationItem | null;
  nextDate: string | null;
  timeZone: string;
}) {
  const t = useTranslations("report");
  const f = useFormat();
  const money = (n: number) => formatMoney(n, currency);
  const p = summary?.primaryItem;
  const primaryRow = stock.find((s) => s.isPrimary);
  const missed = Math.round(p?.lostSalesEstimate ?? 0);
  const carried = stock.filter((s) => (s.carriedOverQty ?? 0) > 0);
  const notes: { label: string; value: ReactNode; detail?: string }[] = [];

  if (p && primaryRow) {
    notes.push({
      label: t("mainItem"),
      value: p.soldOutAt ? t("soldOutAt", { name: p.name, time: formatClock(p.soldOutAt, timeZone) }) : p.name,
      detail: [
        missed > 0 && t("missed", { count: missed }),
        t("sold", { qty: f.quantity(p.soldQty, primaryRow.unit) }),
        primaryRow.recommendedQty !== null && t("suggested", { qty: f.quantity(primaryRow.recommendedQty, primaryRow.unit) }),
      ]
        .filter(Boolean)
        .join(" · "),
    });
  }
  notes.push({
    label: t("carriedTomorrow"),
    value: money(summary?.carriedValue ?? 0),
    detail: carried.length > 0 ? carried.map((s) => `${s.name} ${f.smallQuantity(s.carriedOverQty ?? 0, s.unit)}`).join(", ") : t("nothingCarried"),
  });
  notes.push({
    label: t("voided"),
    value: String(summary?.voidCount ?? voids.length),
    detail:
      voids.length > 0
        ? voids.map((v) => `#${String(v.receiptNo).padStart(3, "0")}${v.reason ? ` · ${v.reason}` : ""}`).join("; ") + t("stockRestored")
        : undefined,
  });
  if (nextRec && nextDate) {
    notes.push({
      label: t("suggestionFor", { day: f.weekday(nextDate) }),
      value: `${nextRec.name} ${f.quantity(nextRec.recommendedQty, nextRec.unit)}`,
      detail:
        t("range", { lower: f.quantity(nextRec.lowerQty, nextRec.unit), upper: f.quantity(nextRec.upperQty, nextRec.unit) }) +
        (nextRec.estimatedCost !== null ? t("estimated", { amount: money(nextRec.estimatedCost) }) : ""),
    });
  }

  return (
    <section aria-labelledby="notes-heading" className={`${card} p-7`}>
      <h2 id="notes-heading" className="text-xl font-semibold">{t("notesTitle")}</h2>
      <dl className="mt-4 divide-y divide-base-300 border-t border-base-300 text-sm">
        {notes.map((n) => (
          <div key={n.label} className="py-3.5">
            <div className="flex justify-between gap-4">
              <dt>{n.label}</dt>
              <dd className="text-right font-semibold">{n.value}</dd>
            </div>
            {n.detail && <p className="mt-1 text-xs text-base-content/70">{n.detail}</p>}
          </div>
        ))}
      </dl>
    </section>
  );
}

/** Grafik batang porsi per jam, satu seri; label angka hanya di jam teramai. */
export function HourlyChart({
  byHour,
  openHour,
  closeHour,
  timeZone,
  soldOutNote,
}: {
  byHour: DailyReport["byHour"];
  openHour: number;
  closeHour: number;
  timeZone: string;
  /** Mis. "Telur sold out at 17:20." */
  soldOutNote: string | null;
}) {
  const t = useTranslations("report");
  const series = hourlySeries(byHour, openHour, closeHour);
  const max = Math.max(1, ...series.map((h) => h.qty));
  const top = busiestHours(series);
  const zone = new Date().toLocaleTimeString("id-ID", { timeZone, timeZoneName: "short" }).split(" ").pop();
  const summary =
    top.length === 0
      ? t("noSales")
      : t("busiestAt", { hours: top.map((h) => t("hourPortions", { hour: hh(h.hour), count: h.qty })).join(t("and")) }) +
        (soldOutNote ? ` ${soldOutNote}` : "");

  return (
    <section aria-labelledby="hourly-heading" className={`${card} p-7`}>
      <h2 id="hourly-heading" className="text-xl font-semibold">{t("hourlyTitle")}</h2>
      <p className="mt-1 text-sm text-base-content/70">{t("hourlyLead", { zone: zone ?? "" })}</p>

      <ol className="mt-8 flex h-52 items-end gap-0.5" aria-label={t("hourlyAria")}>
        {series.map((h) => (
          <li key={h.hour} tabIndex={0} className="group relative flex h-full min-w-0 flex-1 flex-col justify-end outline-none">
            {h.qty === max && h.qty > 0 && (
              <span aria-hidden className="mb-1 text-center text-xs">{h.qty}</span>
            )}
            <span
              aria-hidden
              className="block rounded-t-[4px] bg-secondary transition group-hover:opacity-80 group-focus:opacity-80"
              style={{ height: `${(h.qty / max) * 100}%` }}
            />
            <span className="sr-only">{t("barLabel", { hour: hh(h.hour), count: h.qty })}</span>
            {/* Tooltip per batang saat hover atau fokus keyboard. */}
            <span
              aria-hidden
              className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-1 hidden -translate-x-1/2 whitespace-nowrap rounded-field bg-neutral px-2 py-1 text-xs text-neutral-content group-hover:block group-focus:block"
            >
              {t("barTooltip", { hour: hh(h.hour), count: h.qty })}
            </span>
          </li>
        ))}
      </ol>
      <ol aria-hidden className="mt-2 flex gap-0.5 border-t border-base-300 pt-2 text-center text-xs text-base-content/60">
        {/* Di layar sempit hanya tiap 3 jam yang diberi label supaya angkanya tidak bertumpuk. */}
        {series.map((h, i) => (
          <li key={h.hour} className={`min-w-0 flex-1 ${i % 3 ? "max-sm:invisible" : ""}`}>{hh(h.hour)}</li>
        ))}
      </ol>
      <p className="mt-5 text-sm text-base-content/80">{summary}</p>
    </section>
  );
}

const methods: PaymentMethod[] = ["CASH", "QRIS", "OTHER"];

/** Omzet per metode bayar dengan persentasenya. */
export function PaymentCard({ byMethod, revenue, currency }: { byMethod: DailyReport["byPaymentMethod"]; revenue: number; currency: Currency }) {
  const t = useTranslations("report");
  const tp = useTranslations("payment");
  const money = (n: number) => formatMoney(n, currency);
  return (
    <section aria-labelledby="payment-heading" className={`${card} p-7`}>
      <h2 id="payment-heading" className="text-xl font-semibold">{t("paymentTitle")}</h2>
      <p className="mt-1 text-sm text-base-content/70">{t("paymentLead")}</p>
      <ul className="mt-5 space-y-4 text-sm">
        {methods.map((m) => {
          const amount = byMethod[m] ?? 0;
          const share = pct(amount, revenue);
          return (
            <li key={m}>
              <span className="flex items-baseline justify-between gap-3">
                {tp(m)}
                <span>
                  <strong>{money(amount)}</strong>
                  <span className="ml-2 inline-block w-9 text-right text-xs text-base-content/60">{share}%</span>
                </span>
              </span>
              <span className="mt-2 block">
                <Meter value={share} label={`${tp(m)} ${share}%`} />
              </span>
            </li>
          );
        })}
      </ul>
      <p className="mt-5 flex justify-between border-t border-base-300 pt-4 text-sm">
        {t.rich("totalRevenue", { amount: money(revenue), strong: (chunks) => <strong>{chunks}</strong> })}
      </p>
    </section>
  );
}

/** Penjualan per produk, diurutkan dari omzet terbesar. */
export function ProductTable({ rows, currency }: { rows: DailyReport["byProduct"]; currency: Currency }) {
  const t = useTranslations("report");
  const f = useFormat();
  const money = (n: number) => formatMoney(n, currency);
  const totalQty = rows.reduce((s, r) => s + r.qty, 0);
  const sorted = rows.toSorted((a, b) => b.revenue - a.revenue);
  return (
    <section aria-labelledby="products-heading" className={`${card} overflow-hidden`}>
      <div className="px-7 pt-7 pb-5">
        <CardHeading title={t("productsTitle")} lead={t("productsLead")} />
      </div>
      <div className="relative overflow-x-auto">
        <table className="w-full min-w-2xl text-left text-sm">
          <thead className="border-y border-base-300 bg-base-200/40 text-xs uppercase tracking-widest text-base-content/70">
            <tr>
              <th className="px-7 py-3 font-normal">{t("colProduct")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colPortions")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colRevenue")}</th>
              <th className="w-72 px-7 py-3 text-center font-normal">{t("colShare")}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-base-300">
            {sorted.map((r) => {
              const share = pct(r.qty, totalQty);
              return (
                <tr key={r.productId}>
                  <td className="px-7 py-3">{r.name}</td>
                  <td className="px-3 py-3 text-center">{f.number(r.qty)}</td>
                  <td className="px-3 py-3 text-center font-semibold">{money(r.revenue)}</td>
                  <td className="px-7 py-3">
                    <span className="flex items-center justify-center gap-3">
                      <Meter value={share} label={t("shareLabel", { share })} />
                      <span className="w-9 shrink-0 text-right text-xs text-base-content/60">{share}%</span>
                    </span>
                  </td>
                </tr>
              );
            })}
            {sorted.length === 0 && (
              <tr><td colSpan={4} className="px-7 py-8 text-center text-base-content/70">{t("noProducts")}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

/** Stok tiap bahan dari awal sampai tutup sesi. */
export function StockTable({ stock, timeZone, currency }: { stock: SessionStock[]; timeZone: string; currency: Currency }) {
  const t = useTranslations("report");
  const f = useFormat();
  const money = (n: number) => formatMoney(n, currency);
  const rows = stock.toSorted((a, b) => Number(b.isPrimary) - Number(a.isPrimary));
  const num = "px-3 py-3 text-center";
  return (
    <section aria-labelledby="stock-heading" className={`${card} overflow-hidden`}>
      <div className="px-7 pt-7 pb-5">
        <CardHeading
          title={t("stockTitle")}
          lead={t("stockLead")}
        />
      </div>
      <div className="relative overflow-x-auto">
        <table className="w-full min-w-3xl text-left text-sm">
          <thead className="border-y border-base-300 bg-base-200/40 text-xs uppercase tracking-widest text-base-content/70">
            <tr>
              <th className="px-7 py-3 font-normal">{t("colIngredient")}</th>
              <th className={`${num} font-normal`}>{t("colOpening")}</th>
              <th className={`${num} font-normal`}>{t("colBought")}</th>
              <th className={`${num} font-normal`}>{t("colSold")}</th>
              <th className={`${num} font-normal`}>{t("colCarried")}</th>
              <th className={`${num} font-normal`}>{t("colDiscarded")}</th>
              <th className="px-7 py-3 text-center font-normal">{t("colSoldOut")}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-base-300">
            {rows.map((s) => {
              // Satuan kecil (g, ml) supaya tidak ada nol koma seperti 0,045 kg.
              const q = (v: number | null) => f.smallQuantity(v ?? 0, s.unit);
              const discarded = s.discardedQty ?? 0;
              return (
                <tr key={s.stockItemId}>
                  <td className="px-7 py-3">
                    {s.name}
                    {s.isPrimary && <span className="ml-2 rounded-full bg-base-200 px-2 py-0.5 text-xs">{t("mainItem")}</span>}
                  </td>
                  <td className={num}>{q(s.openingQty)}</td>
                  <td className={num}>{q(s.addedQty)}</td>
                  <td className={num}>{q(s.soldQty)}</td>
                  <td className={`${num} font-semibold`}>{q(s.carriedOverQty)}</td>
                  <td className={`${num} ${discarded > 0 ? "text-error" : ""}`}>
                    {discarded > 0 ? `${q(discarded)} · ${money(s.discardedCost ?? 0)}` : "0"}
                  </td>
                  <td className={`px-7 py-3 text-center ${s.soldOutAt ? "text-error" : "text-base-content/50"}`}>
                    {s.soldOutAt ? t("soldOutTime", { time: formatClock(s.soldOutAt, timeZone) }) : "—"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
