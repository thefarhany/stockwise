import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getLocale, getTranslations } from "next-intl/server";
import type { Product } from "@/features/catalog/types";
import { Cashier } from "@/features/pos/components/cashier";
import { StallStatus } from "@/features/pos/components/stall-status";
import type { SaleSummary } from "@/features/pos/types";
import type { SessionListItem } from "@/features/reports/types";
import type { Session } from "@/features/session/types";
import { hhmm, todayIn } from "@/lib/format";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("cashier")} · Stockwise` };
}

export default async function CashierPage() {
  const [me, current, products] = await Promise.all([
    getProfile(),
    coreGet<Session | null>("/sessions/current"),
    coreGet<Product[]>("/products"),
  ]);
  if (!me?.success || !current?.success || !products?.success) redirect("/login");

  const session = current.data;
  if (!session || session.status !== "OPEN") {
    const { user, outlet } = me.data;
    const today = todayIn(outlet.timezone);
    // Daftar sesi khusus Owner; Cashier cukup melihat status "belum buka".
    const todays = user.role === "OWNER" ? await coreGet<SessionListItem[]>(`/sessions?from=${today}&to=${today}&size=1`) : null;
    const closedToday = todays?.success ? (todays.data.find((x) => x.status === "CLOSED") ?? null) : null;
    return (
      <AppShell profile={me.data} active="cashier">
        <StallStatus currency={outlet.currency} today={today} openTime={hhmm(outlet.openTime)} closeTime={hhmm(outlet.closeTime)} closedToday={closedToday} />
      </AppShell>
    );
  }

  // Nomor struk urut per sesi; transaksi terbaru ada di urutan pertama.
  const latest = await coreGet<SaleSummary[]>(`/sales?sessionId=${session.id}&size=1`);
  const nextReceiptNo = (latest?.success ? (latest.data[0]?.receiptNo ?? 0) : 0) + 1;

  return (
    <AppShell profile={me.data} active="cashier" bare>
      <Cashier
        sessionId={session.id}
        // Produk tanpa resep tidak bisa dijual (stok tidak bisa dikurangi).
        products={products.data.filter((p) => p.recipe.length > 0)}
        stock={session.stock}
        nextReceiptNo={nextReceiptNo}
        timeZone={me.data.outlet.timezone}
        dateLabel={new Date(`${session.businessDate}T00:00:00Z`).toLocaleDateString((await getLocale()) === "en" ? "en-GB" : "id", {
          weekday: "short",
          day: "numeric",
          month: "short",
          timeZone: "UTC",
        })}
      />
    </AppShell>
  );
}
