"use client";

import { useLocale, useTranslations } from "next-intl";
import { useFieldArray, useFormContext, useWatch, type UseFormSetError } from "react-hook-form";
import { LuPlus, LuTrash2 } from "react-icons/lu";
import { useMoney } from "@/components/currency";
import { lineCost, recipeCost } from "@/features/catalog/recipe-cost";
import type { ProductRecipeInput } from "@/features/catalog/schemas";
import type { StockItem, StockUnit } from "@/features/catalog/types";
import { defaultQuantityUnit, quantityUnitsFor, toStockQuantity, unitLabel } from "@/features/catalog/units";

// unit diganti otomatis begitu bahan dipilih.
export const emptyRecipeRow = { stockItemId: "", unit: "GRAM", quantity: Number.NaN } as const;

/** HPP per porsi dari baris resep yang sudah lengkap, dengan harga perkiraan bahan. */
export function useRecipeCost(stockItems: StockItem[]) {
  const { control } = useFormContext<ProductRecipeInput>();
  const locale = useLocale();
  const quantityFormat = new Intl.NumberFormat(locale, { maximumFractionDigits: 3 });
  const rows = useWatch({ control, name: "items" }) ?? [];
  const byId = new Map(stockItems.map((s) => [s.id, s]));
  const lines = rows.flatMap((r) => {
    const item = byId.get(r.stockItemId);
    // Harga per satuan stok, jadi biaya dihitung dari jumlah dalam satuan stok.
    const quantity = item && Number.isFinite(r.quantity) && r.quantity > 0 ? toStockQuantity(r.quantity, r.unit, item.unit) : null;
    return item && quantity !== null
      ? [{ id: r.stockItemId, name: item.name, label: `${quantityFormat.format(r.quantity)} ${unitLabel(r.unit, locale)}`, quantity, unitCost: item.unitCost ?? 0 }]
      : [];
  });
  return { lines, hpp: recipeCost(lines) };
}

/**
 * Baris resep dalam satuan stok, siap dikirim ke PUT /products/{id}/recipe.
 * null bila ada jumlah yang lebih halus dari 0,001 satuan stok; error `tooPrecise(satuan)` dipasang di baris itu.
 */
export function toRecipeRequest(
  items: ProductRecipeInput["items"],
  stockItems: StockItem[],
  setError: UseFormSetError<ProductRecipeInput>,
  tooPrecise: (unit: StockUnit) => string,
) {
  const unitOf = new Map(stockItems.map((s) => [s.id, s.unit]));
  const out: { stockItemId: string; quantity: number }[] = [];
  for (const [i, r] of items.entries()) {
    const stock = unitOf.get(r.stockItemId)!;
    const quantity = toStockQuantity(r.quantity, r.unit, stock);
    if (quantity === null) {
      setError(`items.${i}.quantity`, { message: tooPrecise(stock) }, { shouldFocus: true });
      return null;
    }
    out.push({ stockItemId: r.stockItemId, quantity });
  }
  return { items: out };
}

/** Daftar bahan resep per porsi: bahan, jumlah, satuan, dan (opsional) biaya per baris. */
export function RecipeRows({ stockItems, showCost = false }: { stockItems: StockItem[]; showCost?: boolean }) {
  const money = useMoney();
  const locale = useLocale();
  const t = useTranslations("products");
  const { register, control, setValue, formState: { errors } } = useFormContext<ProductRecipeInput>();
  const { fields, append, remove } = useFieldArray({ control, name: "items" });
  const rows = useWatch({ control, name: "items" });
  const byId = new Map(stockItems.map((s) => [s.id, s]));
  const listError = errors.items?.root?.message ?? errors.items?.message;
  const cols = showCost
    ? "grid-cols-[1fr_5.5rem_5.5rem_4.5rem_2rem]"
    : "grid-cols-[1fr_6rem_6rem_2.5rem] sm:grid-cols-[1fr_8rem_7rem_2.5rem]";

  return (
    <fieldset aria-describedby="recipe-hint">
      <legend className={showCost ? "text-xs uppercase tracking-widest text-base-content/70" : "text-sm font-semibold"}>
        {t("recipePerPortion")}
      </legend>
      <div aria-hidden className={`mt-3 grid ${cols} gap-3 text-xs uppercase tracking-widest text-base-content/70`}>
        <span>{t("colIngredient")}</span>
        <span className="col-span-2">{t("colAmount")}</span>
        {showCost && <span className="text-right">{t("colCost")}</span>}
      </div>
      <ul className="mt-2 space-y-3">
        {fields.map((f, i) => {
          const rowErrors = errors.items?.[i];
          const row = rows?.[i];
          const item = byId.get(row?.stockItemId ?? "");
          const unitOptions = item ? quantityUnitsFor(item.unit) : [];
          const quantity = item && row && Number.isFinite(row.quantity) && row.quantity > 0 ? toStockQuantity(row.quantity, row.unit, item.unit) : null;
          return (
            <li key={f.id}>
              <div className={`grid ${cols} items-center gap-3`}>
                <select
                  aria-label={t("ingredientN", { n: i + 1 })}
                  aria-invalid={rowErrors?.stockItemId ? true : undefined}
                  className="select w-full border-base-300 bg-white/70 aria-invalid:border-error"
                  {...register(`items.${i}.stockItemId`, {
                    // Ganti satuan ke satuan resep bawaan bahan yang baru dipilih (kg → gram).
                    onChange: (e) => {
                      const next = byId.get(e.target.value);
                      if (next) setValue(`items.${i}.unit`, defaultQuantityUnit(next.unit));
                    },
                  })}
                >
                  <option value="" disabled>{t("choose")}</option>
                  {stockItems.map((s) => (
                    <option key={s.id} value={s.id}>{s.name}{s.active ? "" : t("inactiveSuffix")}</option>
                  ))}
                </select>
                <input
                  type="number"
                  inputMode="decimal"
                  step="any"
                  min={0}
                  aria-label={t("amountN", { n: i + 1 })}
                  aria-invalid={rowErrors?.quantity ? true : undefined}
                  placeholder="60"
                  className="w-full rounded-field border border-base-300 bg-white/70 px-3.5 py-2.5 text-sm outline-none placeholder:text-base-content/50 focus:border-base-content aria-invalid:border-error"
                  {...register(`items.${i}.quantity`, { valueAsNumber: true })}
                />
                <select
                  aria-label={t("unitN", { n: i + 1 })}
                  disabled={unitOptions.length < 2}
                  className="select w-full rounded-full border-0 bg-base-200 text-base-content/80 disabled:bg-base-200 disabled:text-base-content/70"
                  {...register(`items.${i}.unit`)}
                >
                  {unitOptions.length === 0 && <option value={row?.unit}>–</option>}
                  {unitOptions.map((u) => <option key={u} value={u}>{unitLabel(u, locale)}</option>)}
                </select>
                {showCost && (
                  <span className="text-right text-sm">
                    {item && quantity !== null ? money(lineCost(quantity, item.unitCost ?? 0)) : "–"}
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => remove(i)}
                  disabled={fields.length === 1}
                  aria-label={t("removeN", { n: i + 1 })}
                  className={`btn btn-ghost btn-square ${showCost ? "btn-sm" : ""}`}
                >
                  <LuTrash2 className="size-4" aria-hidden />
                </button>
              </div>
              {(rowErrors?.stockItemId || rowErrors?.quantity) && (
                <p className="mt-1 text-xs text-error">{rowErrors.stockItemId?.message ?? rowErrors.quantity?.message}</p>
              )}
            </li>
          );
        })}
      </ul>
      <button
        type="button"
        onClick={() => append(emptyRecipeRow)}
        disabled={fields.length >= 50}
        className="mt-3 flex w-full items-center justify-center gap-2 rounded-field border border-dashed border-base-content/40 py-3 text-sm hover:bg-base-200"
      >
        <LuPlus className="size-4" aria-hidden /> {t("addToRecipe")}
      </button>
      <p id="recipe-hint" className={`mt-3 text-xs ${listError ? "text-error" : "text-base-content/60"}`}>
        {listError ?? t("recipeHint")}
      </p>
    </fieldset>
  );
}
