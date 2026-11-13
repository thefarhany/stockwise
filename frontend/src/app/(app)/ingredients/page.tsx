import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { IngredientsManager } from "@/features/catalog/components/ingredients-manager";
import type { StockItem } from "@/features/catalog/types";
import type { OpeningStock, Session } from "@/features/session/types";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("ingredients")} · Stockwise` };
}

export default async function IngredientsPage() {
  const me = await getProfile();
  if (!me?.success) redirect("/login");
  // Ubah bahan khusus Owner; Cashier juga tidak melihat harga.
  if (me.data.user.role !== "OWNER") redirect("/dashboard");

  const [items, current] = await Promise.all([
    coreGet<StockItem[]>("/stock-items?includeInactive=true"),
    coreGet<Session | null>("/sessions/current"),
  ]);
  if (!items?.success) redirect("/login");

  // Lapak buka: stok tersedia sesi ini (berkurang tiap penjualan). Lapak tutup: sisa yang akan jadi stok awal berikutnya.
  const session = current?.success && current.data?.status === "OPEN" ? current.data : null;
  const leftovers = session ? null : await coreGet<OpeningStock[]>("/sessions/opening-stock");
  const stock: Record<string, number> = Object.fromEntries(
    session
      ? session.stock.map((s) => [s.stockItemId, s.availableQty])
      : leftovers?.success
        ? leftovers.data.map((s) => [s.stockItemId, s.quantity])
        : [],
  );

  return (
    <AppShell profile={me.data} active="ingredients">
      <div className="mx-auto max-w-7xl">
        <IngredientsManager items={items.data} stock={stock} stallOpen={session !== null} />
      </div>
    </AppShell>
  );
}
