import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import type { StockItem } from "@/features/catalog/types";
import { CloseForm } from "@/features/session/components/close-form";
import type { Session } from "@/features/session/types";
import { getFormat } from "@/i18n/format-server";
import { formatClock } from "@/lib/format";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("closeStall")} · Stockwise` };
}

export default async function ClosePage() {
  const [me, current, stockItems] = await Promise.all([
    getProfile(),
    coreGet<Session | null>("/sessions/current"),
    coreGet<StockItem[]>("/stock-items?includeInactive=true"),
  ]);
  if (!me?.success || !current?.success || !stockItems?.success) redirect("/login");
  const session = current.data;
  if (!session || session.status !== "OPEN") redirect("/dashboard");
  const tz = me.data.outlet.timezone;
  const t = await getTranslations("close");
  const tc = await getTranslations("common");
  const f = await getFormat();

  return (
    <AppShell profile={me.data} active="home">
      <div className="mx-auto max-w-7xl">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full bg-base-200 px-2.5 py-0.5 text-xs">
              <span aria-hidden className="size-1.5 rounded-full bg-success" /> {t("stallOpen")}
            </span>
            <p className="mt-3 text-xs uppercase tracking-widest text-base-content/70">
              {f.date(session.businessDate)} · {formatClock(new Date().toISOString(), tz)}
            </p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight">{t("title")}</h1>
            <p className="mt-2 text-sm text-base-content/80">{t("lead")}</p>
          </div>
          <Link href="/dashboard" className="btn btn-sm border-base-300 bg-white font-normal">{tc("cancel")}</Link>
        </header>
        <CloseForm
          sessionId={session.id}
          stock={session.stock}
          stockItems={stockItems.data}
          autoCloseLabel={formatClock(session.autoCloseAt, tz)}
        />
      </div>
    </AppShell>
  );
}
