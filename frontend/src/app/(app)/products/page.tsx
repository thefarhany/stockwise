import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { ProductsManager } from "@/features/catalog/components/products-manager";
import type { Product, StockItem } from "@/features/catalog/types";
import type { Outlet } from "@/features/onboarding/types";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("products")} · Stockwise` };
}

export default async function ProductsPage() {
  const me = await getProfile();
  if (!me?.success) redirect("/login");
  // HPP, margin, dan ubah resep khusus Owner.
  if (me.data.user.role !== "OWNER") redirect("/dashboard");

  const [products, stockItems, outlet] = await Promise.all([
    coreGet<Product[]>("/products?includeInactive=true"),
    coreGet<StockItem[]>("/stock-items?includeInactive=true"),
    coreGet<Outlet>("/outlet"),
  ]);
  if (!products?.success || !stockItems?.success || !outlet?.success) redirect("/login");

  return (
    <AppShell profile={me.data} active="products">
      <div className="mx-auto max-w-7xl">
        <ProductsManager products={products.data} stockItems={stockItems.data} targetMarginPct={outlet.data.targetMarginPct} />
      </div>
    </AppShell>
  );
}
