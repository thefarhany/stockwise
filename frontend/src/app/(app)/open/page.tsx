import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { OpenForm } from "@/features/session/components/open-form";
import type { OpeningStock, Recommendation, Session } from "@/features/session/types";
import { getFormat } from "@/i18n/format-server";
import { formatClock, hhmm, todayIn } from "@/lib/format";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("openStall")} · Stockwise` };
}

export default async function OpenPage() {
  const [me, current, opening, rec] = await Promise.all([
    getProfile(),
    coreGet<Session | null>("/sessions/current"),
    coreGet<OpeningStock[]>("/sessions/opening-stock"),
    coreGet<Recommendation>("/recommendations"),
  ]);
  if (!me?.success || !current?.success || !opening?.success) redirect("/login");
  if (current.data) redirect("/dashboard");
  const { outlet } = me.data;
  if (!outlet.setupCompleted) redirect("/dashboard");
  const tz = outlet.timezone;
  const recommendation = rec?.success ? rec.data : null;
  const t = await getTranslations("open");
  const tc = await getTranslations("common");
  const f = await getFormat();

  return (
    <AppShell profile={me.data} active="home">
      <div className="mx-auto max-w-7xl">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-widest text-base-content/70">
              {f.date(recommendation?.targetDate ?? todayIn(tz))} · {formatClock(new Date().toISOString(), tz)}
            </p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight">{t("title")}</h1>
            <p className="mt-2 text-sm text-base-content/80">
              {t("lead", { open: hhmm(outlet.openTime), close: hhmm(outlet.closeTime) })}
            </p>
          </div>
          <Link href="/dashboard" className="btn btn-sm border-base-300 bg-white font-normal">{tc("cancel")}</Link>
        </header>
        <OpenForm
          stock={opening.data}
          suggested={Object.fromEntries((recommendation?.items ?? []).map((i) => [i.stockItemId, i.recommendedQty]))}
        />
      </div>
    </AppShell>
  );
}
