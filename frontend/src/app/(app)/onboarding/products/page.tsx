import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { LuCheck } from "react-icons/lu";
import type { Product, StockItem } from "@/features/catalog/types";
import { CostPanel, ProductRecipeForm, ProductRecipeFormProvider } from "@/features/onboarding";
import type { Outlet } from "@/features/onboarding/types";
import { formatMoney } from "@/lib/currency";
import { coreGet } from "@/server/core-get";
import { OnboardingShell } from "../_components/onboarding-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("onboarding"))("productsMeta")} · Stockwise` };
}

export default async function ProductsPage() {
  const [outlet, products, stockItems] = await Promise.all([
    coreGet<Outlet>("/outlet"),
    coreGet<Product[]>("/products"),
    coreGet<StockItem[]>("/stock-items"),
  ]);
  // Hanya Owner yang boleh menyiapkan usaha; langkah sebelumnya wajib selesai dulu.
  if (outlet && !outlet.success && outlet.errorCode === "FORBIDDEN") redirect("/dashboard");
  if (!outlet?.success || !products?.success || !stockItems?.success) redirect("/login");
  if (!outlet.data.setupCompleted) redirect("/onboarding");
  if (outlet.data.onboarding.stockItems === 0) redirect("/onboarding/ingredients");

  // Produk tanpa resep (mis. resep gagal disimpan) belum bisa dijual, jadi tidak dihitung selesai.
  const added = products.data.filter((p) => p.recipe.length > 0);
  const t = await getTranslations("onboarding");
  const categories = [...new Set(products.data.flatMap((p) => (p.category ? [p.category] : [])))];

  return (
    <ProductRecipeFormProvider>
      <OnboardingShell
        currency={outlet.data.currency}
        current={3}
        title={t("productsTitle")}
        lead={t("productsLead")}
        aside={
          <>
            <CostPanel stockItems={stockItems.data} targetMarginPct={outlet.data.targetMarginPct} />
            {added.length > 0 && (
              <section className="rounded-box border border-base-300 bg-white/50 p-6" aria-labelledby="added-heading">
                <div className="flex items-center justify-between">
                  <h2 id="added-heading" className="text-sm font-semibold">{t("productsAdded")}</h2>
                  <span className="rounded-full bg-base-200 px-3 py-0.5 text-xs">{added.length}</span>
                </div>
                <ul className="mt-4 divide-y divide-base-300 border-t border-base-300">
                  {added.map((p) => (
                    <li key={p.id} className="flex items-start gap-3 py-4">
                      <span className="mt-0.5 grid size-6 shrink-0 place-items-center rounded-full bg-base-content text-base-100">
                        <LuCheck className="size-3.5" aria-hidden />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate">{p.name}</span>
                        <span className="text-xs text-base-content/60">
                          {p.category ? `${p.category} · ` : ""}{formatMoney(p.price, outlet.data.currency)}
                        </span>
                      </span>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </>
        }
      >
        <ProductRecipeForm
          stockItems={stockItems.data}
          categories={categories}
          hasRecipes={added.length > 0}
          targetMarginPct={outlet.data.targetMarginPct}
        />
      </OnboardingShell>
    </ProductRecipeFormProvider>
  );
}
