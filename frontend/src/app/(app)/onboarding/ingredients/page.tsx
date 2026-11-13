import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { LuCheck, LuLightbulb, LuPlus } from "react-icons/lu";
import type { StockItem } from "@/features/catalog/types";
import { stockUnits } from "@/features/catalog/units";
import { IngredientForm } from "@/features/onboarding";
import type { Outlet } from "@/features/onboarding/types";
import { formatMoney } from "@/lib/currency";
import { coreGet } from "@/server/core-get";
import { OnboardingShell, TipCard } from "../_components/onboarding-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("onboarding"))("ingredientsMeta")} · Stockwise` };
}

export default async function IngredientsPage() {
  const [outlet, items] = await Promise.all([coreGet<Outlet>("/outlet"), coreGet<StockItem[]>("/stock-items")]);
  // Hanya Owner yang boleh menyiapkan usaha; profil wajib selesai dulu.
  if (outlet && !outlet.success && outlet.errorCode === "FORBIDDEN") redirect("/dashboard");
  if (!outlet?.success || !items?.success) redirect("/login");
  if (!outlet.data.setupCompleted) redirect("/onboarding");
  const t = await getTranslations("onboarding");

  return (
    <OnboardingShell
      currency={outlet.data.currency}
      current={2}
      title={t("ingredientsTitle")}
      lead={t("ingredientsLead")}
      aside={
        <>
          <section className="rounded-box border border-base-300 bg-white/50 p-6" aria-labelledby="added-heading">
            <div className="flex items-center justify-between border-b border-base-300 pb-4">
              <h2 id="added-heading" className="text-sm font-semibold">{t("ingredientsAdded")}</h2>
              <span className="rounded-full bg-base-200 px-3 py-0.5 text-xs">{items.data.length}</span>
            </div>
            <ul className="divide-y divide-base-300">
              {items.data.map((item) => (
                <li key={item.id} className="flex items-start gap-3 py-4">
                  <span className="mt-0.5 grid size-6 shrink-0 place-items-center rounded-full bg-base-content text-base-100">
                    <LuCheck className="size-3.5" aria-hidden />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate">{item.name}</span>
                    <span className="text-xs text-base-content/60">
                      {stockUnits[item.unit].short} · {formatMoney(item.unitCost ?? 0, outlet.data.currency)}
                    </span>
                  </span>
                  {item.isPrimary && (
                    <span className="shrink-0 rounded-full bg-base-content px-3 py-1 text-xs text-base-100">{t("mainItem")}</span>
                  )}
                </li>
              ))}
            </ul>
            <p className="mt-2 flex items-center gap-2 rounded-field border border-dashed border-base-content/30 px-4 py-3 text-sm text-base-content/60">
              <LuPlus className="size-4" aria-hidden /> {t("nextIngredientHere")}
            </p>
          </section>
          <TipCard icon={<LuLightbulb className="size-4" aria-hidden />} title={t("tipMainTitle")}>
            {t("tipMainText")}
          </TipCard>
        </>
      }
    >
      <IngredientForm hasItems={items.data.length > 0} />
    </OnboardingShell>
  );
}
