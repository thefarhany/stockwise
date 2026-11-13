import type { Metadata } from "next";
import Link from "next/link";
import { LuCalendar } from "react-icons/lu";
import { redirect } from "next/navigation";
import { getLocale, getTranslations } from "next-intl/server";
import { trendRange, rangeStart, MAX_RANGE_DAYS } from "@/features/reports/trend";
import type { RangeReport } from "@/features/reports/types";
import { getFormat } from "@/i18n/format-server";
import { formatMoney } from "@/lib/currency";
import { todayIn } from "@/lib/format";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("trend"))("title")} · Stockwise` };
}

const card = "rounded-box border border-base-300 bg-white/60";
const PRESETS = [7, 30, 90];

export default async function TrendPage({ searchParams }: PageProps<"/reports/trend">) {
  const me = await getProfile();
  if (!me?.success) redirect("/login");
  if (me.data.user.role !== "OWNER") redirect("/dashboard");
  const { currency, timezone } = me.data.outlet;
  const today = todayIn(timezone);
  const { from, to, groupBy } = trendRange(await searchParams, today);

  const res = await coreGet<RangeReport>(`/reports/range?from=${from}&to=${to}&groupBy=${groupBy}`);
  const data = res?.success ? res.data : null;
  const [t, r, f, locale] = await Promise.all([getTranslations("trend"), getTranslations("report"), getFormat(), getLocale()]);
  const money = (n: number) => formatMoney(n, currency);
  const short = (date: string) =>
    new Date(`${date}T00:00:00Z`).toLocaleDateString(locale === "en" ? "en-GB" : locale, { day: "numeric", month: "short", timeZone: "UTC" });
  const periodLabel = (date: string) => (groupBy === "week" ? t("weekOf", { date: short(date) }) : short(date));
  const max = Math.max(1, ...(data?.points.map((p) => p.revenue) ?? []));

  const totals = data && [
    { label: t("sessions"), value: f.number(data.total.sessions) },
    { label: t("portions"), value: f.number(data.total.itemsSold) },
    { label: r("revenue"), value: money(data.total.revenue) },
    { label: r("profit"), value: money(data.total.netProfit) },
  ];
  const field = "input input-sm border-base-300 bg-white";

  return (
    <AppShell profile={me.data} active="reports">
      <div className="mx-auto max-w-7xl">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">{t("title")}</h1>
            <p className="mt-2 text-sm text-base-content/80">{t("lead")} {t("maxRange")}</p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <Link href="/reports" className="btn btn-sm border-base-300 bg-white font-normal">
              <LuCalendar className="size-4" aria-hidden />
              {t("daily")}
            </Link>
            {data && data.total.sessions > 0 && (
              <a href={`/api/exports/sales?from=${from}&to=${to}`} className="btn btn-sm border-base-300 bg-white font-normal">
                {t("downloadExcel")}
              </a>
            )}
          </div>
        </header>

        {/* Form GET biasa: rentang ada di URL, jadi bisa dibagikan dan tidak butuh JavaScript. */}
        <form action="/reports/trend" className={`${card} mt-6 p-5 text-sm`}>
          {/* Isian melebar mengisi baris; tombol cepat rentang di kanan pada layar lebar, di baris sendiri bila sempit. */}
          <div className="flex flex-wrap items-end gap-4">
            <label className="flex min-w-36 flex-1 flex-col gap-1">
              {t("from")}
              <input type="date" name="from" defaultValue={from} min={rangeStart(today, MAX_RANGE_DAYS)} max={today} className={`${field} w-full`} required />
            </label>
            <label className="flex min-w-36 flex-1 flex-col gap-1">
              {t("to")}
              <input type="date" name="to" defaultValue={to} max={today} className={`${field} w-full`} required />
            </label>
            <label className="flex min-w-36 flex-1 flex-col gap-1">
              {t("groupBy")}
              <select name="groupBy" defaultValue={groupBy} className="select select-sm w-full border-base-300 bg-white">
                <option value="day">{t("byDay")}</option>
                <option value="week">{t("byWeek")}</option>
              </select>
            </label>
            <button type="submit" className="btn btn-sm btn-neutral px-6">{t("apply")}</button>
            {/* Berlabel seperti isian lain supaya semua kontrol sejajar di satu garis. */}
            <div className="flex flex-col gap-1 border-base-300 max-xl:w-full xl:border-l xl:pl-4">
              <span>{t("quickRange")}</span>
              <div className="flex flex-wrap gap-2">
                {PRESETS.map((days) => (
                  <Link
                    key={days}
                    href={`/reports/trend?from=${rangeStart(today, days)}&to=${today}&groupBy=${groupBy}`}
                    className="btn btn-sm border-base-300 bg-white font-normal"
                  >
                    {t("lastDays", { days })}
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </form>

        {data && totals && data.points.length > 0 ? (
          <div className="mt-6 space-y-6">
            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {totals.map((x) => (
                <div key={x.label} className={`${card} p-5`}>
                  <dt className="text-xs uppercase tracking-widest text-base-content/70">{x.label}</dt>
                  <dd className="mt-2 text-2xl font-semibold tracking-tight">{x.value}</dd>
                </div>
              ))}
            </dl>

            <section aria-labelledby="trend-chart-heading" className={`${card} p-7`}>
              <h2 id="trend-chart-heading" className="text-xl font-semibold">{t("chartTitle")}</h2>
              <ol className="mt-8 flex h-52 items-end justify-center gap-0.5" aria-label={t("chartAria")}>
                {data.points.map((p) => {
                  const label = t("barLabel", { period: periodLabel(p.period), revenue: money(p.revenue), profit: money(p.netProfit) });
                  return (
                    <li key={p.period} tabIndex={0} className="group relative flex h-full min-w-0 max-w-16 flex-1 flex-col justify-end outline-none">
                      <span
                        aria-hidden
                        className="block rounded-t-[4px] bg-secondary transition group-hover:opacity-80 group-focus:opacity-80"
                        style={{ height: `${(p.revenue / max) * 100}%` }}
                      />
                      <span className="sr-only">{label}</span>
                      <span
                        aria-hidden
                        className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-1 hidden -translate-x-1/2 whitespace-nowrap rounded-field bg-neutral px-2 py-1 text-xs text-neutral-content group-hover:block group-focus:block"
                      >
                        {label}
                      </span>
                    </li>
                  );
                })}
              </ol>
              {/* Lebar kolom sama dengan batang di atas; hanya periode pertama dan terakhir yang diberi label. */}
              <ol aria-hidden className="mt-2 flex justify-center gap-0.5 text-xs text-base-content/70">
                {data.points.map((p, i) => (
                  <li key={p.period} className={`min-w-0 max-w-16 flex-1 whitespace-nowrap ${i === data.points.length - 1 && i > 0 ? "flex justify-end" : ""}`}>
                    {i === 0 || i === data.points.length - 1 ? periodLabel(p.period) : ""}
                  </li>
                ))}
              </ol>
            </section>

            <section className={`${card} relative overflow-x-auto`}>
              <table className="table table-sm">
                <thead>
                  <tr>
                    <th>{t("period")}</th>
                    <th className="text-right">{t("sessions")}</th>
                    <th className="text-right">{t("portions")}</th>
                    <th className="text-right">{r("revenue")}</th>
                    <th className="text-right">{r("cogs")}</th>
                    <th className="text-right">{r("wasted")}</th>
                    <th className="text-right">{r("profit")}</th>
                  </tr>
                </thead>
                <tbody>
                  {data.points.toReversed().map((p) => (
                    <tr key={p.period}>
                      <td className="whitespace-nowrap">
                        {groupBy === "day" ? (
                          <Link href={`/reports?date=${p.period}`} className="link link-hover">{f.date(p.period)}</Link>
                        ) : (
                          periodLabel(p.period)
                        )}
                      </td>
                      <td className="text-right">{f.number(p.sessions)}</td>
                      <td className="text-right">{f.number(p.itemsSold)}</td>
                      <td className="text-right">{money(p.revenue)}</td>
                      <td className="text-right">{money(p.cogs)}</td>
                      <td className="text-right">{money(p.wasteCost)}</td>
                      <td className={`text-right font-semibold ${p.netProfit < 0 ? "text-error" : ""}`}>{money(p.netProfit)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>
        ) : (
          <div className="mt-10 rounded-box border border-base-300 bg-white/60 px-7 py-12 text-center text-sm text-base-content/70">
            {t("noData")}
          </div>
        )}
      </div>
    </AppShell>
  );
}
