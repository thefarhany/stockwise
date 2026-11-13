import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import type { Profile } from "@/features/auth/types";
import type { StockItem } from "@/features/catalog/types";
import { nextOnboardingStep } from "@/features/onboarding/next-step";
import type { Outlet } from "@/features/onboarding/types";
import { SummaryCard } from "@/features/reports/components/summary-card";
import type { SessionListItem, SessionSummary } from "@/features/reports/types";
import {
  LeftoversCard,
  leftoverRowsFromRecommendation,
  leftoverRowsFromSession,
} from "@/features/session/components/leftovers-card";
import { RecommendationCard } from "@/features/session/components/recommendation-card";
import type { Recommendation, Session } from "@/features/session/types";
import { LogoutButton } from "@/components/logout-button";
import { getFormat } from "@/i18n/format-server";
import { formatMoney } from "@/lib/currency";
import { dayBefore, formatClock, hhmm, todayIn } from "@/lib/format";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("dashboard")} · Stockwise` };
}

// shortcut: memakai jam asli, bukan jam simulasi backend; cukup untuk sapaan.
function greetingKey(timeZone: string) {
  const hour = Number(new Date().toLocaleTimeString("en-GB", { hour: "2-digit", hourCycle: "h23", timeZone }));
  return hour < 12 ? "morning" : hour < 18 ? "afternoon" : "evening";
}

export default async function DashboardPage() {
  const me = await getProfile();
  if (!me?.success) redirect("/login");

  const { user, outlet } = me.data;
  const t = await getTranslations("dashboard");
  const f = await getFormat();
  if (user.role !== "OWNER") {
    // Cashier tidak bisa melihat hitungan onboarding (GET /outlet khusus Owner).
    if (!outlet.setupCompleted) {
      return (
        <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col justify-center gap-6 px-6 py-16">
          <p className="text-xs uppercase tracking-widest text-base-content/70">{outlet.name}</p>
          <h1 className="text-3xl font-semibold tracking-tight">{t("notSetUp")}</h1>
          <p className="text-sm text-base-content/80">{t("askOwner")}</p>
          <div><LogoutButton /></div>
        </main>
      );
    }
    return <CashierHome profile={me.data} />;
  }

  const [full, current, rec, history, stockItems] = await Promise.all([
    coreGet<Outlet>("/outlet"),
    coreGet<Session | null>("/sessions/current"),
    coreGet<Recommendation>("/recommendations"),
    coreGet<SessionListItem[]>("/sessions?size=2"),
    coreGet<StockItem[]>("/stock-items"),
  ]);
  if (!full?.success) redirect("/login");
  // Owner menyelesaikan semua langkah onboarding dulu; kembali ke langkah yang belum selesai.
  const step = nextOnboardingStep(full.data.onboarding);
  if (step) redirect(step);

  const tz = full.data.timezone;
  const session = current?.success ? current.data : null;
  const todayRec = rec?.success ? rec.data : null;
  const today = session?.businessDate ?? todayRec?.targetDate ?? todayIn(tz);

  // Sesi tertutup terakhir: sumber ringkasan dan sisa yang dibawa ke hari ini.
  const lastClosed = history?.success ? history.data.find((s) => s.status === "CLOSED") : undefined;
  const [closedSession, summary] = lastClosed
    ? await Promise.all([
        coreGet<Session>(`/sessions/${lastClosed.id}`),
        coreGet<SessionSummary>(`/sessions/${lastClosed.id}/summary`),
      ])
    : [null, null];
  const last = closedSession?.success ? closedSession.data : null;
  const lastSummary = summary?.success ? summary.data : null;

  const open = hhmm(full.data.openTime);
  const close = hhmm(full.data.closeTime);
  const closedToday = last?.businessDate === today;

  // Lapak hari ini sudah tutup: yang perlu disiapkan adalah belanja besok.
  const nextDate = closedToday ? lastSummary?.nextDayRecommendationDate : null;
  const nextRec = nextDate ? await coreGet<Recommendation>(`/recommendations?date=${nextDate}`) : null;
  const recommendation = nextDate ? (nextRec?.success ? nextRec.data : null) : todayRec;
  const status = session
    ? t("openSince", { since: formatClock(session.openedAt, tz), close })
    : t("notOpen", { open, close });
  const primaryIds = new Set(stockItems?.success ? stockItems.data.filter((i) => i.isPrimary).map((i) => i.id) : []);
  const carriedValue = last?.stock.reduce((t, r) => t + (r.carriedValue ?? 0), 0) ?? 0;

  const recommendationCard = (
    <RecommendationCard
      currency={outlet.currency}
      rec={recommendation}
      title={closedToday ? t("suggestionFor", { day: nextDate ? f.weekday(nextDate) : t("tomorrow") }) : t("todaySuggestion")}
      primaryIds={primaryIds}
      soldOut={{ primary: lastSummary?.primaryItem ?? null, timeZone: tz }}
    />
  );
  const summaryCard = lastSummary && (
    <SummaryCard
      currency={outlet.currency}
      summary={lastSummary}
      closedAt={last?.closedAt ?? null}
      timeZone={tz}
      title={closedToday ? t("todaySummary") : lastSummary.businessDate === dayBefore(today) ? t("yesterdaySummary") : t("latestSummary")}
      primaryStock={last?.stock.find((r) => r.isPrimary)}
    />
  );

  return (
    <AppShell profile={me.data} active="home">
      <div className="mx-auto max-w-7xl">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-widest text-base-content/70">
              {f.date(today)}
              {closedToday && ` · ${formatClock(new Date().toISOString(), tz)}`}
            </p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight">
              {closedToday ? t("stallClosed") : t("greeting", { greeting: t(greetingKey(tz)), name: user.name.split(/\s+/)[0] })}
            </h1>
            <p className="mt-2 text-sm text-base-content/80">
              {closedToday && last?.closedAt
                ? t(last.closeType === "AUTO" ? "closedAutoAt" : "closedAt", { time: formatClock(last.closedAt, tz) })
                : status}
            </p>
          </div>
          {session ? (
            <Link href="/close" className="btn btn-primary">{t("closeStall")}</Link>
          ) : closedToday ? (
            <Link href={`/reports?date=${today}`} className="btn btn-primary">{t("dailyReport")}</Link>
          ) : (
            <Link href="/open" className="btn btn-primary">{t("openStall")}</Link>
          )}
        </header>

        <div className={`mt-6 grid gap-6 ${session ? "" : "xl:grid-cols-[1fr_22rem]"}`}>
          <div className="min-w-0 space-y-6">
            {/* Setelah tutup: ringkasan dulu, lalu saran untuk besok. Sebelum buka: saran hari ini dulu. */}
            {closedToday ? (
              <>
                {summaryCard}
                {recommendationCard}
              </>
            ) : (
              <>
                {recommendationCard}
                {summaryCard}
              </>
            )}
          </div>

          {!session && (
            <div className="space-y-6">
              {last && (
                <LeftoversCard
                  rows={leftoverRowsFromSession(last.stock, tz, outlet.currency, f)}
                  title={closedToday ? t("carriedTomorrow") : t("yesterdayLeftovers")}
                  lead={closedToday ? t("becomesOpening", { amount: formatMoney(carriedValue, outlet.currency) }) : t("alreadyRecorded")}
                />
              )}
              {closedToday ? (
                <section className="rounded-box bg-base-200 p-6">
                  <h2 className="text-lg font-semibold">{t("aiTitle")}</h2>
                  <p className="mt-2 text-sm text-base-content/80">{t("aiText")}</p>
                </section>
              ) : (
                <section className="rounded-box bg-base-200 p-6">
                  <h2 className="text-lg font-semibold">{t("quickOpenTitle")}</h2>
                  <p className="mt-2 text-sm text-base-content/80">{t("quickOpenOwner")}</p>
                </section>
              )}
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
}

/**
 * Beranda Cashier: hanya data yang boleh dibaca Cashier (sesi berjalan dan saran belanja tanpa biaya).
 * shortcut: Cashier tidak bisa tahu lapak hari ini sudah ditutup (daftar sesi khusus Owner), jadi tampil "belum dibuka".
 */
async function CashierHome({ profile }: { profile: Profile }) {
  const { user, outlet } = profile;
  const tz = outlet.timezone;
  const [current, rec, stockItems] = await Promise.all([
    coreGet<Session | null>("/sessions/current"),
    coreGet<Recommendation>("/recommendations"),
    coreGet<StockItem[]>("/stock-items"),
  ]);
  const session = current?.success ? current.data : null;
  const recommendation = rec?.success ? rec.data : null;
  const today = session?.businessDate ?? recommendation?.targetDate ?? todayIn(tz);
  const primaryIds = new Set(stockItems?.success ? stockItems.data.filter((i) => i.isPrimary).map((i) => i.id) : []);
  const t = await getTranslations("dashboard");
  const f = await getFormat();

  return (
    <AppShell profile={profile} active="home">
      <div className="mx-auto max-w-7xl">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-widest text-base-content/70">{f.date(today)}</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight">
              {t("greeting", { greeting: t(greetingKey(tz)), name: user.name.split(/\s+/)[0] })}
            </h1>
            <p className="mt-2 text-sm text-base-content/80">
              {session
                ? t("openSince", { since: formatClock(session.openedAt, tz), close: hhmm(outlet.closeTime) })
                : t("notOpen", { open: hhmm(outlet.openTime), close: hhmm(outlet.closeTime) })}
            </p>
          </div>
          {session ? (
            <div className="flex flex-wrap gap-3">
              <Link href="/cashier" className="btn border-base-300 bg-white">{t("openCashier")}</Link>
              <Link href="/close" className="btn btn-primary">{t("closeStall")}</Link>
            </div>
          ) : (
            <Link href="/open" className="btn btn-primary">{t("openStall")}</Link>
          )}
        </header>

        <div className={`mt-6 grid gap-6 ${session ? "" : "xl:grid-cols-[1fr_22rem]"}`}>
          <div className="min-w-0">
            <RecommendationCard
              currency={outlet.currency}
              rec={recommendation}
              title={t("todaySuggestion")}
              primaryIds={primaryIds}
              soldOut={{ primary: null, timeZone: tz }}
            />
          </div>
          {!session && (
            <div className="space-y-6">
              {recommendation && (
                <LeftoversCard
                  rows={leftoverRowsFromRecommendation(recommendation, f)}
                  title={t("yesterdayLeftovers")}
                  lead={t("alreadyRecorded")}
                />
              )}
              <section className="rounded-box bg-base-200 p-6">
                <h2 className="text-lg font-semibold">{t("quickOpenTitle")}</h2>
                <p className="mt-2 text-sm text-base-content/80">{t("quickOpenCashier")}</p>
              </section>
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
}
