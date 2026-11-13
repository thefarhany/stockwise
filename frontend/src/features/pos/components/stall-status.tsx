import Link from "next/link";
import { useTranslations } from "next-intl";
import { LuClock, LuLock, LuStore } from "react-icons/lu";
import type { SessionListItem } from "@/features/reports/types";
import { useFormat } from "@/i18n/format";
import { formatMoney, type Currency } from "@/lib/currency";

/**
 * Layar kasir saat tidak ada sesi terbuka: belum dibuka, atau sudah ditutup hari ini.
 * `closedToday` hanya terisi untuk Owner (daftar sesi khusus Owner).
 */
export function StallStatus({
  currency,
  today,
  openTime,
  closeTime,
  closedToday,
}: {
  /** Mata uang usaha. */
  currency: Currency;
  today: string;
  openTime: string;
  closeTime: string;
  closedToday: SessionListItem | null;
}) {
  const t = useTranslations("stallStatus");
  const f = useFormat();
  const steps = [1, 2, 3].map((n) => ({ title: t(`step${n}Title` as "step1Title"), text: t(`step${n}Text` as "step1Text") }));
  const money = (n: number) => formatMoney(n, currency);
  const closed = closedToday !== null;
  const stats = closed
    ? [
        { label: t("revenue"), value: closedToday.revenue !== null ? money(closedToday.revenue) : "–" },
        { label: t("profit"), value: closedToday.netProfit !== null ? money(closedToday.netProfit) : "–" },
        ...(closedToday.primaryItem
          ? [{ label: t("sold", { name: closedToday.primaryItem.name }), value: f.number(closedToday.primaryItem.soldQty) }]
          : []),
      ]
    : [];

  return (
    <div className="mx-auto max-w-5xl">
      <header className="flex flex-wrap items-center gap-4">
        <h1 className="text-3xl font-semibold tracking-tight">{t("title")}</h1>
        <span className="flex items-center gap-2 rounded-full bg-base-200 px-2.5 py-0.5 text-xs">
          <span aria-hidden className="size-1.5 rounded-full bg-base-content/40" />
          {closed ? t("closed") : t("notOpen")} · {f.date(today)}
        </span>
      </header>

      <section aria-labelledby="stall-status-heading" className="mt-6 overflow-hidden rounded-box border border-base-300 bg-white/60">
        <div className="grid grid-cols-1 gap-8 px-8 py-10 md:grid-cols-[auto_1fr] md:items-start">
          <span className="grid size-14 place-items-center rounded-full bg-base-200">
            {closed ? <LuLock className="size-6" aria-hidden /> : <LuStore className="size-6" aria-hidden />}
          </span>
          <div>
            <h2 id="stall-status-heading" className="text-2xl font-semibold tracking-tight">
              {closed ? t("closedTitle") : t("notOpenTitle")}
            </h2>
            <p className="mt-2 max-w-xl text-sm text-base-content/80">
              {closed ? t("closedText") : t("notOpenText")}
            </p>

            <p className="mt-4 flex items-center gap-2 text-sm text-base-content/70">
              <LuClock className="size-4" aria-hidden /> {t("hours", { open: openTime, close: closeTime })}
            </p>

            {stats.length > 0 && (
              <dl className="mt-6 grid max-w-xl grid-cols-2 gap-3 sm:grid-cols-3">
                {stats.map((s) => (
                  <div key={s.label} className="rounded-box bg-base-200/70 px-4 py-3">
                    <dt className="text-xs text-base-content/70">{s.label}</dt>
                    <dd className="mt-1 font-semibold">{s.value}</dd>
                  </div>
                ))}
              </dl>
            )}

            <div className="mt-6 flex flex-wrap gap-3">
              {closed ? (
                <>
                  <Link href={`/reports?date=${today}`} className="btn btn-primary btn-sm">{t("seeReport")}</Link>
                  <Link href="/transactions" className="btn btn-sm border-base-300 bg-white">{t("history")}</Link>
                </>
              ) : (
                <Link href="/dashboard" className="btn btn-primary btn-sm">{t("goHome")}</Link>
              )}
            </div>
          </div>
        </div>

        <ol className="grid grid-cols-1 border-t border-base-300 bg-base-200/40 sm:grid-cols-3">
          {steps.map((s, i) => {
            // Langkah 1 berlaku sebelum buka; setelah tutup ketiganya sudah dilalui hari ini.
            const done = closed;
            const current = !closed && i === 0;
            return (
              <li key={s.title} className="flex gap-3 border-base-300 px-6 py-5 not-first:border-t sm:not-first:border-t-0 sm:not-first:border-l">
                <span
                  aria-hidden
                  className={`grid size-6 shrink-0 place-items-center rounded-full text-xs ${
                    done || current ? "bg-base-content text-base-100" : "bg-base-300 text-base-content/70"
                  }`}
                >
                  {done ? "✓" : i + 1}
                </span>
                <span>
                  <span className={`block text-sm font-semibold ${current ? "" : "text-base-content/80"}`}>{s.title}</span>
                  <span className="mt-0.5 block text-xs text-base-content/60">{s.text}</span>
                </span>
              </li>
            );
          })}
        </ol>
      </section>
    </div>
  );
}
