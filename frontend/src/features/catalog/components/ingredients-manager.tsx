"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { LuPencil, LuRotateCcw, LuTrash2, LuX } from "react-icons/lu";
import { useMoney } from "@/components/currency";
import { Modal } from "@/components/ui/modal";
import { FormError } from "@/features/auth/components/form-error";
import { createStockItem, deactivateStockItem, updateStockItem } from "@/features/catalog/api/stock-items";
import { StockItemFields, stockItemDefaults } from "@/features/catalog/components/stock-item-fields";
import { stockItemSchema, type StockItemInput } from "@/features/catalog/schemas";
import type { StockItem } from "@/features/catalog/types";
import { stockUnits } from "@/features/catalog/units";
import { ApiError, errorMessage } from "@/lib/api-client";

const FORM_ID = "stock-item-form";

const toInput = (i: StockItem): StockItemInput => ({
  name: i.name,
  kind: i.kind,
  unit: i.unit,
  unitCost: i.unitCost ?? 0,
  shelfLifeDays: i.shelfLifeDays,
  carryOverAllowed: i.carryOverAllowed,
  isPrimary: i.isPrimary,
});


/** Tabel bahan dengan pencarian, tambah/ubah lewat modal, dan nonaktifkan/aktifkan kembali. */
export function IngredientsManager({ items }: { items: StockItem[] }) {
  const money = useMoney();
  const t = useTranslations("ingredients");
  const tc = useTranslations("catalog");
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [showInactive, setShowInactive] = useState(false);
  // undefined = modal tertutup, null = bahan baru.
  const [editing, setEditing] = useState<StockItem | null | undefined>(undefined);
  const [removing, setRemoving] = useState<StockItem | null>(null);
  const [rowError, setRowError] = useState<string | null>(null);

  const q = query.trim().toLowerCase();
  const shown = items.filter((i) => (showInactive || i.active) && (q === "" || i.name.toLowerCase().includes(q)));
  const activeCount = items.filter((i) => i.active).length;

  async function reactivate(item: StockItem) {
    setRowError(null);
    try {
      await updateStockItem(item.id, { ...toInput(item), active: true });
      router.refresh();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      setRowError(e instanceof ApiError ? e.message : errorMessage(""));
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
          <span className="text-sm text-base-content/80">{t("activeCount", { count: activeCount })}</span>
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
              <th className="px-5 py-3 font-normal">{t("colName")}</th>
              <th className="px-3 py-3 font-normal">{t("colType")}</th>
              <th className="px-3 py-3 font-normal">{t("colUnit")}</th>
              <th className="px-3 py-3 text-right font-normal">{t("colPrice")}</th>
              <th className="px-3 py-3 text-right font-normal">{t("colShelfLife")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colCarryOver")}</th>
              <th className="px-5 py-3 text-right font-normal">{t("colActions")}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-base-300">
            {shown.map((i) => (
              <tr key={i.id} className={`hover:bg-base-200/60 ${i.active ? "" : "text-base-content/50"}`}>
                <td className="px-5 py-3 font-semibold">
                  {i.name}
                  {i.isPrimary && <span className="ml-2 rounded-full bg-base-content px-2 py-0.5 text-xs font-normal text-base-100">{t("mainItem")}</span>}
                  {!i.active && <span className="ml-2 rounded-full bg-base-200 px-2 py-0.5 text-xs font-normal">{t("inactive")}</span>}
                </td>
                <td className="px-3 py-3">{tc(`kind.${i.kind}`)}</td>
                <td className="px-3 py-3">{stockUnits[i.unit].short}</td>
                <td className="px-3 py-3 text-right">{money(i.unitCost ?? 0)} / {stockUnits[i.unit].short}</td>
                <td className="px-3 py-3 text-right">{t("days", { count: i.shelfLifeDays })}</td>
                <td className={`px-3 py-3 text-center ${i.carryOverAllowed ? "" : "text-base-content/60"}`}>{i.carryOverAllowed ? t("yes") : t("no")}</td>
                <td className="px-5 py-3">
                  <span className="flex justify-end gap-1">
                    {i.active ? (
                      <>
                        <button type="button" className="btn btn-ghost btn-sm btn-square" aria-label={t("editName", { name: i.name })} onClick={() => setEditing(i)}>
                          <LuPencil className="size-4" aria-hidden />
                        </button>
                        <button type="button" className="btn btn-ghost btn-sm btn-square" aria-label={t("deactivateName", { name: i.name })} onClick={() => setRemoving(i)}>
                          <LuTrash2 className="size-4" aria-hidden />
                        </button>
                      </>
                    ) : (
                      <button type="button" className="btn btn-ghost btn-sm font-normal" onClick={() => reactivate(i)}>
                        <LuRotateCcw className="size-4" aria-hidden /> {t("reactivate")}
                      </button>
                    )}
                  </span>
                </td>
              </tr>
            ))}
            {shown.length === 0 && (
              <tr><td colSpan={7} className="px-5 py-10 text-center text-base-content/70">{t("noMatch")}</td></tr>
            )}
          </tbody>
        </table>
      </div>
      <p className="mt-4 text-xs text-base-content/70">
        {t("estimateNote")}
      </p>

      <Modal open={editing !== undefined} onClose={() => setEditing(undefined)} labelledBy="stock-item-title" className="max-w-4xl">
        {editing !== undefined && (
          <StockItemDialog
            // Ganti bahan = form baru dengan nilai awal bahan itu.
            key={editing?.id ?? "new"}
            item={editing}
            onDone={() => {
              setEditing(undefined);
              router.refresh();
            }}
            onCancel={() => setEditing(undefined)}
            onDeactivate={(i) => {
              setEditing(undefined);
              setRemoving(i);
            }}
          />
        )}
      </Modal>

      <Modal open={removing !== null} onClose={() => setRemoving(null)} labelledBy="deactivate-title">
        {removing && (
          <DeactivateDialog
            key={removing.id}
            item={removing}
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

function StockItemDialog({
  item,
  onDone,
  onCancel,
  onDeactivate,
}: {
  item: StockItem | null;
  onDone: () => void;
  onCancel: () => void;
  onDeactivate: (item: StockItem) => void;
}) {
  const router = useRouter();
  const t = useTranslations("ingredients");
  const tc = useTranslations("common");
  const tu = useTranslations("units");
  const tv = useTranslations("validation");
  const form = useForm<StockItemInput>({ resolver: zodResolver(stockItemSchema(tv)), defaultValues: item ? toInput(item) : stockItemDefaults });
  const { handleSubmit, setError, setValue, formState: { errors, isSubmitting } } = form;
  // Backend tidak memberi tahu di awal; satuan baru terkunci setelah ditolak karena bahan sudah dipakai di sesi.
  const [unitLocked, setUnitLocked] = useState(false);

  const save = handleSubmit(async (input) => {
    try {
      if (item) await updateStockItem(item.id, input);
      else await createStockItem(input);
      onDone();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      if (e instanceof ApiError && e.errorCode === "DUPLICATE_NAME") {
        setError("name", { message: t("duplicate") }, { shouldFocus: true });
      } else if (item && e instanceof ApiError && e.details?.fields?.unit) {
        setValue("unit", item.unit);
        setUnitLocked(true);
        setError("root", {
          message: t("unitLockedError", { unit: tu(`names.${item.unit}`) }),
        });
      } else {
        setError("root", { message: e instanceof ApiError ? e.message : errorMessage("") });
      }
    }
  });

  return (
    <>
      <div className="flex items-start justify-between gap-4 border-b border-base-300 px-6 py-5">
        <div>
          <h2 id="stock-item-title" className="text-xl font-semibold">{item ? t("editTitle") : t("add")}</h2>
          {item && <p className="text-xs text-base-content/70">{[item.name, item.isPrimary && t("mainItemLower"), stockUnits[item.unit].short].filter(Boolean).join(" · ")}</p>}
        </div>
        <button type="button" className="btn btn-ghost btn-sm btn-square" aria-label={tc("close")} onClick={onCancel}>
          <LuX className="size-5" aria-hidden />
        </button>
      </div>

      <form id={FORM_ID} onSubmit={save} noValidate className="max-h-[70vh] space-y-4 overflow-y-auto px-6 py-6">
        <FormError message={errors.root?.message} />
        <StockItemFields form={form} columns unitLocked={unitLocked} />
      </form>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-base-300 bg-base-200/40 px-6 py-4">
        {item ? (
          <button type="button" className="text-sm text-error hover:underline" onClick={() => onDeactivate(item)}>
            {t("deactivateIngredient")}
          </button>
        ) : (
          <span />
        )}
        <div className="flex gap-3">
          <button type="button" className="btn border-base-300 bg-white" onClick={onCancel}>{tc("cancel")}</button>
          <button type="submit" form={FORM_ID} className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? tc("saving") : item ? t("saveChanges") : t("add")}
          </button>
        </div>
      </div>
    </>
  );
}

function DeactivateDialog({ item, onDone, onCancel }: { item: StockItem; onDone: () => void; onCancel: () => void }) {
  const router = useRouter();
  const t = useTranslations("ingredients");
  const tc = useTranslations("common");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function confirm() {
    setPending(true);
    setError(null);
    try {
      await deactivateStockItem(item.id);
      onDone();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      if (e instanceof ApiError && e.errorCode === "STOCK_ITEM_IN_USE" && e.details?.products?.length) {
        setError(t("inUse", { products: e.details.products.join(", ") }));
      } else {
        setError(e instanceof ApiError ? e.message : errorMessage(""));
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <div className="px-6 py-5">
        <h2 id="deactivate-title" className="text-xl font-semibold">{t("deactivateTitle", { name: item.name })}</h2>
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
