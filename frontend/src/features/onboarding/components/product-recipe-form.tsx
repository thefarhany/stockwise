"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useFormat } from "@/i18n/format";
import { useState, type FormEvent, type ReactNode } from "react";
import { FormProvider, useForm, useFormContext, useWatch } from "react-hook-form";
import { useCurrency, useMoney } from "@/components/currency";
import { Field } from "@/components/ui/field";
import { MoneyField } from "@/components/ui/money-field";
import { FormError } from "@/features/auth/components/form-error";
import { createProduct, saveRecipe, updateProduct } from "@/features/catalog/api/products";
import { emptyRecipeRow, RecipeRows, toRecipeRequest, useRecipeCost } from "@/features/catalog/components/recipe-fields";
import { lineCost, suggestPrice } from "@/features/catalog/recipe-cost";
import { productRecipeSchema, type ProductRecipeInput } from "@/features/catalog/schemas";
import type { StockItem } from "@/features/catalog/types";
import { ApiError } from "@/lib/api-client";
import { ONBOARDING_FORM_ID } from "../constants";
import { ADD_MORE, submitIntent, useStepSubmit } from "./use-step-submit";

// Harga sengaja tanpa nilai awal supaya isian kosong, bukan 0.
const defaults = { name: "", category: "", items: [emptyRecipeRow] };
const suggestedCategories = ["bread", "cake", "drinks", "food"] as const;

/** Satu form untuk isian produk (tengah) dan panel HPP (kanan). */
export function ProductRecipeFormProvider({ children }: { children: ReactNode }) {
  const tv = useTranslations("validation");
  const form = useForm<ProductRecipeInput>({ resolver: zodResolver(productRecipeSchema(tv)), defaultValues: defaults });
  return <FormProvider {...form}>{children}</FormProvider>;
}

export function ProductRecipeForm({
  stockItems,
  categories,
  hasRecipes,
  targetMarginPct,
}: {
  stockItems: StockItem[];
  targetMarginPct: number;
  /** Kategori yang sudah dipakai produk lain, ditawarkan bersama saran bawaan. */
  categories: string[];
  /** Sudah ada produk beresep, jadi langkah ini boleh dilewati. */
  hasRecipes: boolean;
}) {
  const money = useMoney();
  const currency = useCurrency();
  const t = useTranslations("products");
  const f = useFormat();
  const to = useTranslations("onboarding");
  const tc = useTranslations("common");
  const { register, control, getValues, setValue, reset, setFocus, handleSubmit, setError, formState: { errors, isSubmitting } } =
    useFormContext<ProductRecipeInput>();
  const category = useWatch({ control, name: "category" });
  const suggested = suggestPrice(useRecipeCost(stockItems).hpp, targetMarginPct, currency);
  const categoryOptions = [...new Set([...suggestedCategories.map((c) => t(`categories.${c}`)), ...categories])];
  // Produk yang sudah dibuat tapi resepnya gagal disimpan; simpan ulang memperbarui produk ini, bukan membuat baru.
  const [createdId, setCreatedId] = useState<string | null>(null);
  const step = useStepSubmit({
    next: "/onboarding/done",
    hasItems: hasRecipes,
    isEmpty: () => !getValues("name").trim() && getValues("items").every((r) => !r.stockItemId && Number.isNaN(r.quantity)),
  });

  const save = handleSubmit(async (input, event) => {
    // Backend menyimpan jumlah resep dalam satuan stok bahan.
    const recipe = toRecipeRequest(input.items, stockItems, setError, (unit) => t("tooPrecise", { unit: f.unit(unit) }));
    if (!recipe) return;

    const product = { name: input.name, category: input.category || null, price: input.price };
    try {
      let id = createdId;
      if (id) await updateProduct(id, product);
      else {
        id = (await createProduct(product)).id;
        setCreatedId(id);
      }
      await saveRecipe(id, recipe);
    } catch (e) {
      if (e instanceof ApiError && e.errorCode === "DUPLICATE_NAME") {
        setError("name", { message: t("duplicateOnboarding") }, { shouldFocus: true });
      } else {
        const message = e instanceof ApiError && e.errorCode === "STOCK_ITEM_NOT_FOUND"
          ? t("ingredientGone")
          : step.failed(e);
        if (message) setError("root", { message });
      }
      return;
    }
    setCreatedId(null);
    if (step.saved(submitIntent(event))) {
      reset(defaults);
      setFocus("name");
    }
  });

  const onSubmit = (e: FormEvent<HTMLFormElement>) => (step.skip(e) ? undefined : save(e));

  return (
    <form id={ONBOARDING_FORM_ID} onSubmit={onSubmit} noValidate className="space-y-6">
      <FormError message={errors.root?.message} />

      <Field id="name" label={t("productName")} placeholder={t("productNamePlaceholder")} error={errors.name?.message} {...register("name")} />

      <div>
        <Field
          id="category"
          label={t("category")}
          placeholder={t("categoryPlaceholder")}
          list="category-options"
          autoComplete="off"
          error={errors.category?.message}
          {...register("category")}
        />
        <datalist id="category-options">
          {categoryOptions.map((c) => <option key={c} value={c} />)}
        </datalist>
        <ul className="mt-2 flex flex-wrap gap-2" aria-label={t("suggestedCategories")}>
          {categoryOptions.map((c) => {
            const active = category.trim().toLowerCase() === c.toLowerCase();
            return (
              <li key={c}>
                <button
                  type="button"
                  aria-pressed={active}
                  onClick={() => setValue("category", active ? "" : c, { shouldValidate: true })}
                  className={`btn btn-sm rounded-full ${active ? "btn-primary" : "border-base-300 bg-white"}`}
                >
                  {c}
                </button>
              </li>
            );
          })}
        </ul>
        <p className="mt-2 text-xs text-base-content/60">{t("categoryHint")}</p>
      </div>

      <RecipeRows stockItems={stockItems} />

      <div className="flex items-start gap-3">
        <MoneyField
          id="price"
          control={control}
          name="price"
          className="flex-1"
          label={t("sellingPrice")}
          hint={t("priceHint", { margin: targetMarginPct })}
          error={errors.price?.message}
        />
        {suggested !== null && (
          // mt menyamakan tombol dengan kotak isian, di bawah label.
          <button
            type="button"
            onClick={() => setValue("price", suggested, { shouldValidate: true })}
            className="btn mt-[1.625rem] shrink-0 border-base-300 bg-white"
          >
            {t("use", { price: money(suggested) })}
          </button>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link href="/onboarding/ingredients" className="btn border-base-300 bg-white">{tc("back")}</Link>
        <div className="flex flex-wrap gap-3">
          <button type="submit" value={ADD_MORE} disabled={isSubmitting} className="btn border-base-300 bg-white">
            {to("saveAddAnother")}
          </button>
          <button type="submit" disabled={isSubmitting} className="btn btn-primary">
            {isSubmitting ? tc("saving") : t("saveAndFinish")}
          </button>
        </div>
      </div>
    </form>
  );
}

/** Panel gelap: HPP per porsi, rincian per bahan, margin, dan saran harga, dihitung ulang setiap kali isian berubah. */
export function CostPanel({ stockItems, targetMarginPct }: { stockItems: StockItem[]; targetMarginPct: number }) {
  const money = useMoney();
  const currency = useCurrency();
  const t = useTranslations("products");
  const { control } = useFormContext<ProductRecipeInput>();
  const price = useWatch({ control, name: "price" });
  const { lines, hpp } = useRecipeCost(stockItems);
  const hasPrice = Number.isFinite(price) && price >= 0;
  const margin = hasPrice ? price - hpp : null;
  const suggested = suggestPrice(hpp, targetMarginPct, currency);

  return (
    <section aria-labelledby="cost-heading" aria-live="polite" className="rounded-box bg-neutral p-8 text-neutral-content">
      <h2 id="cost-heading" className="text-xs uppercase tracking-widest text-neutral-content/70">{t("estimatedCostPerPortion")}</h2>
      <p className="mt-4 text-5xl font-semibold tracking-tight">{money(hpp)}</p>
      <dl className="mt-6 divide-y divide-neutral-content/15 border-t border-neutral-content/15 text-sm">
        {lines.map((l) => (
          <div key={l.id} className="flex justify-between gap-4 py-4">
            <dt className="text-neutral-content/70">{l.name}, {l.label}</dt>
            <dd>{money(lineCost(l.quantity, l.unitCost))}</dd>
          </div>
        ))}
        <div className="flex justify-between gap-4 py-4">
          <dt>{t("sellingPrice")}</dt>
          <dd className="font-semibold">{hasPrice ? money(price) : "–"}</dd>
        </div>
        <div className="flex justify-between gap-4 py-4">
          <dt>{t("margin")}</dt>
          <dd className="font-semibold">
            {margin === null ? "–" : `${money(margin)}${price > 0 ? ` (${Math.round((margin / price) * 100)}%)` : ""}`}
          </dd>
        </div>
      </dl>
      {suggested !== null && (
        <div className="mt-4 rounded-box bg-base-100 p-5 text-base-content">
          <p className="text-xs text-base-content/70">{t("suggestedWithMargin", { margin: targetMarginPct })}</p>
          <p className="mt-1 text-2xl font-semibold">{money(suggested)}</p>
        </div>
      )}
      <p className="mt-6 text-xs text-neutral-content/70">
        {t("estimateNote")}
      </p>
    </section>
  );
}
