import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { IngredientsManager } from "@/features/catalog/components/ingredients-manager";
import type { StockItem } from "@/features/catalog/types";
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

  const items = await coreGet<StockItem[]>("/stock-items?includeInactive=true");
  if (!items?.success) redirect("/login");

  return (
    <AppShell profile={me.data} active="ingredients">
      <div className="mx-auto max-w-7xl">
        <IngredientsManager items={items.data} />
      </div>
    </AppShell>
  );
}
