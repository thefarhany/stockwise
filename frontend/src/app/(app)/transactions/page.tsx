import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { DateFilter } from "@/features/pos/components/date-filter";
import { SalesTable } from "@/features/pos/components/sales-table";
import type { SaleSummary } from "@/features/pos/types";
import type { SessionListItem } from "@/features/reports/types";
import type { Session } from "@/features/session/types";
import type { ApiPage } from "@/lib/api-types";
import { getFormat } from "@/i18n/format-server";
import { todayIn } from "@/lib/format";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("transactions")} · Stockwise` };
}

const PAGE_SIZE = 10;

/** Nomor halaman yang ditampilkan: paling banyak 5, di sekitar halaman aktif. */
function pageNumbers(current: number, total: number) {
  const start = Math.max(1, Math.min(current - 2, total - 4));
  return Array.from({ length: Math.min(5, total) }, (_, i) => start + i);
}

export default async function TransactionsPage({ searchParams }: PageProps<"/transactions">) {
  const params = await searchParams;
  const me = await getProfile();
  if (!me?.success) redirect("/login");
  const { user, outlet } = me.data;
  const isOwner = user.role === "OWNER";

  // Cashier hanya boleh melihat sesi yang sedang berjalan, jadi tanggal hanya bisa dipilih Owner.
  const today = todayIn(outlet.timezone);
  const asked = typeof params.date === "string" && /^\d{4}-\d{2}-\d{2}$/.test(params.date) ? params.date : today;
  const date = isOwner && asked <= today ? asked : today;
  const page = Math.max(1, Number.parseInt(typeof params.page === "string" ? params.page : "1", 10) || 1);

  const [sales, sessionInfo] = await Promise.all([
    coreGet<SaleSummary[]>(`/sales?${isOwner ? `date=${date}&` : ""}page=${page - 1}&size=${PAGE_SIZE}`),
    isOwner
      ? coreGet<SessionListItem[]>(`/sessions?from=${date}&to=${date}&size=1`)
      : coreGet<Session | null>("/sessions/current"),
  ]);
  if (!sales?.success) redirect("/login");
  // GET /sales berhalaman: info halaman ada di amplop, di luar `data`.
  const { data: rows, totalElements, totalPages } = sales as ApiPage<SaleSummary>;

  const status = !sessionInfo?.success
    ? "NONE"
    : Array.isArray(sessionInfo.data)
      ? (sessionInfo.data[0]?.status ?? "NONE")
      : (sessionInfo.data?.status ?? "NONE");
  const href = (p: number) => {
    const q = new URLSearchParams();
    if (date !== today) q.set("date", date);
    if (p > 1) q.set("page", String(p));
    const s = q.toString();
    return `/transactions${s ? `?${s}` : ""}` as const;
  };
  const from = (page - 1) * PAGE_SIZE + 1;
  const t = await getTranslations("transactions");
  const f = await getFormat();

  return (
    <AppShell profile={me.data} active="transactions">
      <div className="mx-auto max-w-7xl">
        <header className="flex flex-wrap items-end justify-between gap-6">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">{t("title")}</h1>
            <p className="mt-2 flex flex-wrap items-center gap-3 text-sm text-base-content/80">
              {f.date(date)}
              <span className="flex items-center gap-2 rounded-full bg-base-200 px-2.5 py-0.5 text-xs">
                <span
                  aria-hidden
                  className={`size-2 rounded-full ${status === "OPEN" ? "bg-success" : "bg-base-content/40"}`}
                />
                {t(status)}
              </span>
            </p>
          </div>
          {isOwner && <DateFilter date={date} today={today} />}
        </header>

        <SalesTable rows={rows} timeZone={outlet.timezone} canVoid={status === "OPEN"} />

        {totalElements > 0 && (
          <nav aria-label={t("pages")} className="mt-4 flex flex-wrap items-center justify-between gap-4">
            <p className="text-sm text-base-content/80">
              {t("showing", { from, to: Math.min(from + rows.length - 1, totalElements), total: totalElements })}
            </p>
            {totalPages > 1 && (
              <div className="flex flex-wrap gap-2">
                {page > 1 ? (
                  <Link href={href(page - 1)} className="btn btn-sm border-base-300 bg-white font-normal">{t("previous")}</Link>
                ) : (
                  <span aria-disabled="true" className="btn btn-sm btn-disabled font-normal">{t("previous")}</span>
                )}
                {pageNumbers(page, totalPages).map((n) => (
                  <Link
                    key={n}
                    href={href(n)}
                    aria-current={n === page ? "page" : undefined}
                    className={`btn btn-sm btn-square font-normal ${n === page ? "btn-primary" : "border-base-300 bg-white"}`}
                  >
                    {n}
                  </Link>
                ))}
                {page < totalPages ? (
                  <Link href={href(page + 1)} className="btn btn-sm border-base-300 bg-white font-normal">{t("next")}</Link>
                ) : (
                  <span aria-disabled="true" className="btn btn-sm btn-disabled font-normal">{t("next")}</span>
                )}
              </div>
            )}
          </nav>
        )}
      </div>
    </AppShell>
  );
}
