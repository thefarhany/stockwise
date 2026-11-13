import type { Metadata } from "next";
import Link from "next/link";
import { LuTrendingUp } from "react-icons/lu";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import type { SaleDetail, SaleSummary } from "@/features/pos/types";
import {
  HourlyChart,
  NotesCard,
  PaymentCard,
  ProductTable,
  ProfitCard,
  StockTable,
} from "@/features/reports/components/daily-report";
import { DateNav } from "@/features/reports/components/date-nav";
import type { DailyReport, SessionListItem, SessionSummary } from "@/features/reports/types";
import type { Recommendation, Session } from "@/features/session/types";
import { formatClock, todayIn } from "@/lib/format";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("reports")} · Stockwise` };
}

/** Paling banyak sekian transaksi batal yang alasannya diambil untuk catatan. */
const VOID_REASONS_SHOWN = 3;

export default async function ReportsPage({ searchParams }: PageProps<"/reports">) {
  const params = await searchParams;
  const me = await getProfile();
  if (!me?.success) redirect("/login");
  if (me.data.user.role !== "OWNER") redirect("/dashboard");
  const { outlet } = me.data;
  const tz = outlet.timezone;
  const today = todayIn(tz);

  // Tanpa ?date=: tampilkan sesi tertutup terakhir, karena laporan hanya ada setelah lapak ditutup.
  let date = typeof params.date === "string" && /^\d{4}-\d{2}-\d{2}$/.test(params.date) && params.date <= today ? params.date : null;
  if (!date) {
    const recent = await coreGet<SessionListItem[]>("/sessions?size=10");
    date = (recent?.success ? recent.data.find((s) => s.status === "CLOSED")?.businessDate : undefined) ?? today;
  }

  const [report, sessionList] = await Promise.all([
    coreGet<DailyReport>(`/reports/daily?date=${date}`),
    coreGet<SessionListItem[]>(`/sessions?from=${date}&to=${date}&size=1`),
  ]);
  const item = sessionList?.success ? sessionList.data[0] : undefined;
  const data = report?.success ? report.data : null;

  const [session, summary, sales] =
    data && item
      ? await Promise.all([
          coreGet<Session>(`/sessions/${item.id}`),
          coreGet<SessionSummary>(`/sessions/${item.id}/summary`),
          coreGet<SaleSummary[]>(`/sales?sessionId=${item.id}&size=100`),
        ])
      : [null, null, null];
  const s = session?.success ? session.data : null;
  const sum = summary?.success ? summary.data : null;

  const voided = sales?.success ? sales.data.filter((x) => x.status === "VOID") : [];
  const [voidDetails, nextRec] = await Promise.all([
    Promise.all(voided.slice(0, VOID_REASONS_SHOWN).map((v) => coreGet<SaleDetail>(`/sales/${v.id}`))),
    sum?.nextDayRecommendationDate
      ? coreGet<Recommendation>(`/recommendations?date=${sum.nextDayRecommendationDate}`)
      : Promise.resolve(null),
  ]);
  const voids = voided.slice(0, VOID_REASONS_SHOWN).map((v, i) => {
    const d = voidDetails[i];
    return { receiptNo: v.receiptNo, reason: d?.success ? d.data.voidReason : null };
  });
  const primaryId = s?.stock.find((x) => x.isPrimary)?.stockItemId;
  const nextPrimary = nextRec?.success ? (nextRec.data.items.find((i) => i.stockItemId === primaryId) ?? null) : null;

  const p = sum?.primaryItem;
  const hour = (time: string | null, fallback: number) => (time ? Number(time.slice(0, 2)) : fallback);
  const t = await getTranslations("reports");

  return (
    <AppShell profile={me.data} active="reports">
      <div className="mx-auto max-w-7xl">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">{t("title")}</h1>
            <p className="mt-2 flex flex-wrap items-center gap-3 text-sm text-base-content/80">
              {s?.closedAt
                ? t("closedAt", { time: formatClock(s.closedAt, tz) })
                : item?.status === "OPEN"
                  ? t("stillOpen")
                  : t("noClosed")}
              {s?.closeType && (
                <span className="flex items-center gap-2 rounded-full bg-base-200 px-2.5 py-0.5 text-xs">
                  <span aria-hidden className="size-1.5 rounded-full bg-success" />
                  {s.closeType === "AUTO" ? t("closedAuto") : t("closedManual")}
                </span>
              )}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <Link href="/reports/trend" className="btn btn-sm border-base-300 bg-white font-normal">
              <LuTrendingUp className="size-4" aria-hidden />
              {t("viewTrend")}
            </Link>
            <DateNav date={date} today={today} />
            {data && (
              <a href={`/api/exports/sales?from=${date}&to=${date}`} className="btn btn-sm border-base-300 bg-white font-normal">
                {t("downloadExcel")}
              </a>
            )}
          </div>
        </header>

        {data ? (
          <div className="mt-6 space-y-6">
            <div className="grid grid-cols-1 gap-6 xl:grid-cols-[1.75fr_1fr]">
              <ProfitCard currency={me.data.outlet.currency} report={data} portions={sum?.totalItemsSold ?? data.byProduct.reduce((n, r) => n + r.qty, 0)} />
              <NotesCard currency={me.data.outlet.currency}
                summary={sum}
                stock={s?.stock ?? []}
                voids={voids}
                nextRec={nextPrimary}
                nextDate={sum?.nextDayRecommendationDate ?? null}
                timeZone={tz}
              />
            </div>
            <div className="grid grid-cols-1 gap-6 xl:grid-cols-[1.75fr_1fr]">
              <HourlyChart
                byHour={data.byHour}
                openHour={hour(outlet.openTime, 7)}
                closeHour={hour(outlet.closeTime, 20)}
                timeZone={tz}
                soldOutNote={p?.soldOutAt ? t("soldOutNote", { name: p.name, time: formatClock(p.soldOutAt, tz) }) : null}
              />
              <PaymentCard currency={me.data.outlet.currency} byMethod={data.byPaymentMethod} revenue={data.revenue} />
            </div>
            <ProductTable currency={me.data.outlet.currency} rows={data.byProduct} />
            {s && <StockTable currency={me.data.outlet.currency} stock={s.stock} timeZone={tz} />}
          </div>
        ) : (
          <div className="mt-10 rounded-box border border-base-300 bg-white/60 px-7 py-12 text-center text-sm text-base-content/70">
            {t("noReport")}
          </div>
        )}
      </div>
    </AppShell>
  );
}
