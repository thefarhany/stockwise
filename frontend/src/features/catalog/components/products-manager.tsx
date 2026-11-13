"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useFormat } from "@/i18n/format";
import { useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { LuPencil, LuRotateCcw, LuTrash2, LuX } from "react-icons/lu";
import { useCurrency, useMoney } from "@/components/currency";
import { Field } from "@/components/ui/field";
import { Modal } from "@/components/ui/modal";
import { MoneyField } from "@/components/ui/money-field";
import { FormError } from "@/features/auth/components/form-error";
import { createProduct, deactivateProduct, saveRecipe, updateProduct } from "@/features/catalog/api/products";
import { emptyRecipeRow, RecipeRows, toRecipeRequest, useRecipeCost } from "@/features/catalog/components/recipe-fields";
import { suggestPrice } from "@/features/catalog/recipe-cost";
import { productRecipeSchema, type ProductRecipeInput } from "@/features/catalog/schemas";
import type { Product, StockItem } from "@/features/catalog/types";
import { defaultQuantityUnit, fromStockQuantity } from "@/features/catalog/units";
import { ApiError, errorMessage } from "@/lib/api-client";

const FORM_ID = "product-form";
const suggestedCategories = ["bread", "cake", "drinks", "food"] as const;

const pct = (part: number, whole: number) => (whole > 0 ? Math.round((part / whole) * 100) : 0);
/** 57 → "+57%", -12 → "-12%". */
const signedPct = (n: number) => `${n > 0 ? "+" : ""}${n}%`;

/** Resep tersimpan (satuan stok) → baris form dalam satuan resep bawaan (kg → gram). */
function toInput(p: Product): ProductRecipeInput {
  return {
    name: p.name,
    category: p.category ?? "",
    price: p.price,
    items:
      p.recipe.length > 0
        ? p.recipe.map((r) => {
            const unit = defaultQuantityUnit(r.unit);
            return { stockItemId: r.stockItemId, unit, quantity: fromStockQuantity(r.quantity, unit, r.unit) };
          })
        : [emptyRecipeRow],
  };
}

function errorText(e: unknown) {
  if (!(e instanceof ApiError)) console.error(e);
  return e instanceof ApiError ? e.message : errorMessage("");
}

/** Tabel produk dengan HPP, margin, dan saran harga; tambah/ubah produk beserta resepnya lewat modal. */
export function ProductsManager({
  products,
  stockItems,
  targetMarginPct,
}: {
  products: Product[];
  /** Termasuk bahan nonaktif, supaya resep lama yang memakainya tetap terbaca. */
  stockItems: StockItem[];
  targetMarginPct: number;
}) {
  const money = useMoney();
  const t = useTranslations("products");
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [showInactive, setShowInactive] = useState(false);
  // undefined = modal tertutup, null = produk baru.
  const [editing, setEditing] = useState<Product | null | undefined>(undefined);
  const [removing, setRemoving] = useState<Product | null>(null);
  const [rowError, setRowError] = useState<string | null>(null);

  const q = query.trim().toLowerCase();
  const shown = products.filter((p) => (showInactive || p.active) && (q === "" || p.name.toLowerCase().includes(q)));
  const active = products.filter((p) => p.active);
  const noRecipe = active.filter((p) => p.recipe.length === 0).length;
  const categories = [...new Set([...suggestedCategories.map((c) => t(`categories.${c}`)), ...products.flatMap((p) => (p.category ? [p.category] : []))])];

  async function reactivate(p: Product) {
    setRowError(null);
    try {
      await updateProduct(p.id, { name: p.name, category: p.category, price: p.price, stockMode: p.stockMode, active: true });
      router.refresh();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      setRowError(errorText(e));
    }
  }

  return (
    <>
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">{t("title")}</h1>
          <p className="mt-2 text-sm text-base-content/80">{t("lead")}</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setEditing(null)}>{t("add")}</button>
      </header>

      <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-4">
          <input
            type="search"
            aria-label={t("search")}
            placeholder={t("search")}
            className="input input-sm w-full bg-white/60 sm:w-72"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <span className="text-sm text-base-content/80">
            {t("count", { count: active.length })}{noRecipe > 0 && t("withoutRecipe", { count: noRecipe })}
          </span>
        </div>
        <label className="flex cursor-pointer items-center gap-3 text-sm">
          <input type="checkbox" role="switch" className="toggle toggle-sm" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} />
          {t("showInactive")}
        </label>
      </div>
      {rowError && <p className="mt-3 text-sm text-error" role="alert">{rowError}</p>}

      <div className="relative mt-4 overflow-x-auto rounded-box border border-base-300 bg-white/60">
        <table className="w-full min-w-3xl text-left text-sm">
          <thead className="border-b border-base-300 bg-base-200/40 text-xs uppercase tracking-widest text-base-content/70">
            <tr>
              <th className="px-5 py-3 font-normal">{t("colProduct")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colPrice")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colHpp")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colMargin")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colSuggested")}</th>
              <th className="px-5 py-3 text-center font-normal">{t("colActions")}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-base-300">
            {shown.map((p) => {
              const hasRecipe = p.recipe.length > 0;
              const marginPct = p.margin !== null ? pct(p.margin, p.price) : null;
              return (
                <tr key={p.id} className={`hover:bg-base-200/60 ${p.active ? "" : "text-base-content/50"}`}>
                  <td className="px-5 py-3">
                    <span className="block font-semibold">{p.name}</span>
                    <span className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-base-content/70">
                      {[p.category, hasRecipe && t("ingredientsCount", { count: p.recipe.length })].filter(Boolean).join(" · ")}
                      {!hasRecipe && (
                        <span className="flex items-center gap-1.5 rounded-full bg-base-200 px-2 py-0.5 text-error">
                          <span aria-hidden className="size-1.5 rounded-full bg-error" /> {t("noRecipe")}
                        </span>
                      )}
                      {p.hppEstimated && <span className="rounded-full bg-base-200 px-2 py-0.5">{t("estimate")}</span>}
                      {!p.active && <span className="rounded-full bg-base-200 px-2 py-0.5">{t("inactive")}</span>}
                    </span>
                  </td>
                  <td className="px-3 py-3 text-center font-semibold">{money(p.price)}</td>
                  <td className="px-3 py-3 text-center">{p.hpp !== null && hasRecipe ? money(p.hpp) : "—"}</td>
                  <td className="px-3 py-3 text-center">
                    {p.margin !== null && hasRecipe ? `${money(p.margin)} (${signedPct(marginPct ?? 0)})` : "—"}
                  </td>
                  <td className="px-3 py-3 text-center">{p.suggestedPrice !== null ? money(p.suggestedPrice) : "—"}</td>
                  <td className="px-5 py-3">
                    <span className="flex justify-center gap-1">
                      {p.active ? (
                        <>
                          <button type="button" className="btn btn-ghost btn-sm btn-square" aria-label={t("editName", { name: p.name })} onClick={() => setEditing(p)}>
                            <LuPencil className="size-4" aria-hidden />
                          </button>
                          <button type="button" className="btn btn-ghost btn-sm btn-square" aria-label={t("deactivateName", { name: p.name })} onClick={() => setRemoving(p)}>
                            <LuTrash2 className="size-4" aria-hidden />
                          </button>
                        </>
                      ) : (
                        <button type="button" className="btn btn-ghost btn-sm font-normal" onClick={() => reactivate(p)}>
                          <LuRotateCcw className="size-4" aria-hidden /> {t("reactivate")}
                        </button>
                      )}
                    </span>
                  </td>
                </tr>
              );
            })}
            {shown.length === 0 && (
              <tr><td colSpan={6} className="px-5 py-10 text-center text-base-content/70">{t("noMatch")}</td></tr>
            )}
          </tbody>
        </table>
      </div>
      <p className="mt-4 text-xs text-base-content/70">
        {t("tableNote", { margin: targetMarginPct })}
      </p>

      <Modal open={editing !== undefined} onClose={() => setEditing(undefined)} labelledBy="product-title" className="max-w-5xl">
        {editing !== undefined && (
          <ProductDialog
            key={editing?.id ?? "new"}
            product={editing}
            stockItems={stockItems}
            categories={categories}
            targetMarginPct={targetMarginPct}
            onDone={() => {
              setEditing(undefined);
              router.refresh();
            }}
            onCancel={() => setEditing(undefined)}
            onDeactivate={(p) => {
              setEditing(undefined);
              setRemoving(p);
            }}
          />
        )}
      </Modal>

      <Modal open={removing !== null} onClose={() => setRemoving(null)} labelledBy="deactivate-product-title">
        {removing && (
          <DeactivateProductDialog
            key={removing.id}
            product={removing}
            onDone={() => {
              setRemoving(null);
              router.refresh();
            }}
            onCancel={() => setRemoving(null)}
          />
        )}
      </Modal>
    </>
  );
}

function ProductDialog({
  product,
  stockItems,
  categories,
  targetMarginPct,
  onDone,
  onCancel,
  onDeactivate,
}: {
  product: Product | null;
  stockItems: StockItem[];
  categories: string[];
  targetMarginPct: number;
  onDone: () => void;
  onCancel: () => void;
  onDeactivate: (p: Product) => void;
}) {
  const router = useRouter();
  const t = useTranslations("products");
  const f = useFormat();
  const tc = useTranslations("common");
  const tv = useTranslations("validation");
  const form = useForm<ProductRecipeInput>({
    resolver: zodResolver(productRecipeSchema(tv)),
    // Harga produk baru sengaja tanpa nilai awal supaya isian kosong, bukan 0.
    defaultValues: product ? toInput(product) : { name: "", category: "", items: [emptyRecipeRow] },
  });
  const { register, control, handleSubmit, setError, setValue, formState: { errors, isSubmitting } } = form;
  // Hanya bahan aktif yang bisa dipilih, kecuali yang sudah ada di resep ini.
  const inRecipe = new Set(product?.recipe.map((r) => r.stockItemId));
  const options = stockItems.filter((s) => s.active || inRecipe.has(s.id));
  // Produk baru yang sudah tersimpan tapi resepnya gagal; simpan ulang memperbarui produk ini, bukan membuat baru.
  const [createdId, setCreatedId] = useState<string | null>(null);

  const save = handleSubmit(async (input) => {
    const recipe = toRecipeRequest(input.items, stockItems, setError, (unit) => t("tooPrecise", { unit: f.unit(unit) }));
    if (!recipe) return;
    const body = { name: input.name, category: input.category || null, price: input.price, stockMode: product?.stockMode };
    try {
      let id = product?.id ?? createdId;
      if (id) await updateProduct(id, body);
      else {
        id = (await createProduct(body)).id;
        setCreatedId(id);
      }
      await saveRecipe(id, recipe);
      onDone();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      if (e instanceof ApiError && e.errorCode === "DUPLICATE_NAME") {
        setError("name", { message: t("duplicate") }, { shouldFocus: true });
      } else if (e instanceof ApiError && e.errorCode === "STOCK_ITEM_NOT_FOUND") {
        setError("root", { message: t("ingredientInactive") });
      } else {
        setError("root", { message: errorText(e) });
      }
    }
  });

  return (
    <FormProvider {...form}>
      <div className="flex items-start justify-between gap-4 border-b border-base-300 px-6 py-5">
        <div>
          <h2 id="product-title" className="text-xl font-semibold">{product ? product.name : t("add")}</h2>
          <p className="text-xs text-base-content/70">{product ? t("editSubtitle") : t("newSubtitle")}</p>
        </div>
        <button type="button" className="btn btn-ghost btn-sm btn-square" aria-label={tc("close")} onClick={onCancel}>
          <LuX className="size-5" aria-hidden />
        </button>
      </div>

      <div className="grid grid-cols-1 max-h-[70vh] gap-8 overflow-y-auto px-6 py-6 md:grid-cols-[1fr_18rem]">
        <form id={FORM_ID} onSubmit={save} noValidate className="min-w-0 space-y-6">
          <FormError message={errors.root?.message} />
          <div className="space-y-4">
            <p className="text-xs uppercase tracking-widest text-base-content/70">{t("productLabel")}</p>
            <Field id="name" label={t("productName")} placeholder={t("productNamePlaceholder")} error={errors.name?.message} {...register("name")} />
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <Field
                  id="category"
                  label={t("category")}
                  placeholder={t("categoryPlaceholder")}
                  list="product-categories"
                  autoComplete="off"
                  error={errors.category?.message}
                  {...register("category")}
                />
                <datalist id="product-categories">
                  {categories.map((c) => <option key={c} value={c} />)}
                </datalist>
              </div>
              <MoneyField id="price" control={control} name="price" label={t("sellingPrice")} error={errors.price?.message} />
            </div>
          </div>
          <RecipeRows stockItems={options} showCost />
        </form>

        <aside className="space-y-3">
          <CostSummary stockItems={stockItems} targetMarginPct={targetMarginPct} onUseSuggestion={(v) => setValue("price", v, { shouldValidate: true })} />
          <p className="text-xs text-base-content/70">
            {t("recalcNote")}
          </p>
        </aside>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-base-300 bg-base-200/40 px-6 py-4">
        {product ? (
          <button type="button" className="text-sm text-error hover:underline" onClick={() => onDeactivate(product)}>
            {t("deactivateProduct")}
          </button>
        ) : (
          <span />
        )}
        <div className="flex gap-3">
          <button type="button" className="btn border-base-300 bg-white" onClick={onCancel}>{tc("cancel")}</button>
          <button type="submit" form={FORM_ID} className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? tc("saving") : product ? t("saveChanges") : t("add")}
          </button>
        </div>
      </div>
    </FormProvider>
  );
}

/** Panel gelap: HPP per porsi, harga jual, margin, dan saran harga, dihitung ulang setiap isian berubah. */
function CostSummary({
  stockItems,
  targetMarginPct,
  onUseSuggestion,
}: {
  stockItems: StockItem[];
  targetMarginPct: number;
  onUseSuggestion: (price: number) => void;
}) {
  const money = useMoney();
  const currency = useCurrency();
  const t = useTranslations("products");
  const price = useWatch<ProductRecipeInput, "price">({ name: "price" });
  const { hpp } = useRecipeCost(stockItems);
  const hasPrice = Number.isFinite(price) && price >= 0;
  const margin = hasPrice ? price - hpp : null;
  const suggested = suggestPrice(hpp, targetMarginPct, currency);

  return (
    <section aria-labelledby="cost-summary-heading" aria-live="polite" className="rounded-box bg-neutral p-6 text-neutral-content">
      <h3 id="cost-summary-heading" className="text-xs uppercase tracking-widest text-neutral-content/70">{t("hppPerPortion")}</h3>
      <p className="mt-2 text-3xl font-semibold tracking-tight">{money(hpp)}</p>
      <dl className="mt-4 divide-y divide-neutral-content/20 border-t border-neutral-content/20 text-sm">
        <div className="flex justify-between gap-4 py-3">
          <dt>{t("sellingPrice")}</dt>
          <dd className="font-semibold">{hasPrice ? money(price) : "–"}</dd>
        </div>
        <div className="flex justify-between gap-4 py-3">
          <dt>{t("margin")}</dt>
          <dd className="font-semibold">
            {margin === null ? "–" : `${money(margin)}${price > 0 ? ` (${signedPct(pct(margin, price))})` : ""}`}
          </dd>
        </div>
        <div className="flex items-center justify-between gap-4 py-3">
          <dt>{t("suggestedMargin", { margin: targetMarginPct })}</dt>
          <dd className="flex items-center gap-2 font-semibold">
            {suggested === null ? "–" : money(suggested)}
            {suggested !== null && suggested !== price && (
              <button type="button" className="rounded-full border border-neutral-content/50 px-2 py-0.5 text-xs font-normal hover:bg-neutral-content/10" onClick={() => onUseSuggestion(suggested)}>
                {t("useShort")}
              </button>
            )}
          </dd>
        </div>
      </dl>
    </section>
  );
}

function DeactivateProductDialog({ product, onDone, onCancel }: { product: Product; onDone: () => void; onCancel: () => void }) {
  const router = useRouter();
  const t = useTranslations("products");
  const tc = useTranslations("common");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function confirm() {
    setPending(true);
    setError(null);
    try {
      await deactivateProduct(product.id);
      onDone();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      setError(errorText(e));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <div className="px-6 py-5">
        <h2 id="deactivate-product-title" className="text-xl font-semibold">{t("deactivateTitle", { name: product.name })}</h2>
        <p className="mt-2 text-sm text-base-content/70">
          {t("deactivateText")}
        </p>
        {error && <p className="mt-3 text-sm text-error" role="alert">{error}</p>}
      </div>
      <div className="flex justify-end gap-3 border-t border-base-300 px-6 py-4">
        <button type="button" className="btn border-base-300 bg-white" onClick={onCancel}>{tc("cancel")}</button>
        <button type="button" className="btn btn-error" disabled={pending} onClick={confirm}>
          {pending ? t("deactivating") : t("deactivate")}
        </button>
      </div>
    </>
  );
}
