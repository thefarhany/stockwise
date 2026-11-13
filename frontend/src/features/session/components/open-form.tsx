"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { LuArrowLeft, LuArrowRight, LuCheck } from "react-icons/lu";
import { useCurrency, useMoney } from "@/components/currency";
import { moneyInputProps, typeMoney } from "@/components/ui/money-field";
import { SelectMenu } from "@/components/ui/select-menu";
import type { StockUnit } from "@/features/catalog/types";
import { convertQuantity, purchaseUnitsFor, stockUnits } from "@/features/catalog/units";
import { openSession } from "@/features/session/api/sessions";
import { parseCount } from "@/features/session/components/close-form";
import type { OpeningStock, StockLine } from "@/features/session/types";
import { useFormat } from "@/i18n/format";
import { ApiError, errorMessage } from "@/lib/api-client";
import { currencies, parseMoney, type Currency } from "@/lib/currency";

// Batas sama dengan backend dto/session/StockLineRequest (Money.MAX_AMOUNT).
const MAX_PAID = 1_000_000_000_000;

/** Isian tambahan satu bahan pagi ini: jumlah dan total bayar (teks dalam mata uang usaha, mis. "50.000" atau "12.50"). */
export type AddRow = { qty: string; unit: StockUnit; paid: string };
/** message = kunci pesan di messages open.*. */
type RowError = { field: "qty" | "paid"; message: "qtyInvalid" | "paidRequired" | "paidTooLarge" };

/** Isian jadi baris belanja untuk backend, pesan salah, atau keduanya null bila tidak ada tambahan. */
export function toStockLine(stock: OpeningStock, row: AddRow, currency: Currency): { line: StockLine | null; error: RowError | null } {
  const paid = parseMoney(row.paid, currency) || 0;
  if (row.qty.trim() === "" && paid === 0) return { line: null, error: null };
  const qty = parseCount(row.qty);
  if (qty === null || qty === 0) return { line: null, error: { field: "qty", message: "qtyInvalid" } };
  if (paid === 0) return { line: null, error: { field: "paid", message: "paidRequired" } };
  if (paid > MAX_PAID) return { line: null, error: { field: "paid", message: "paidTooLarge" } };
  return {
    line: {
      stockItemId: stock.stockItemId,
      quantity: qty,
      unit: row.unit === stock.unit ? undefined : row.unit,
      totalCost: paid,
    },
    error: null,
  };
}

const round3 = (n: number) => Math.round(n * 1000) / 1000;

/** Buka lapak dua langkah: lihat stok semua bahan dan isi tambahan pagi ini, lalu periksa dan buka. */
export function OpenForm({ stock, suggested }: { stock: OpeningStock[]; /** Saran belanja per bahan (satuan bahan). */ suggested: Record<string, number> }) {
  const money = useMoney();
  const currency = useCurrency();
  const t = useTranslations("open");
  const f = useFormat();
  const steps = [t("stepStock"), t("stepReview")];
  const router = useRouter();
  const items = stock.toSorted(
    (a, b) => Number(b.isPrimary) - Number(a.isPrimary) || Number(b.active) - Number(a.active) || a.name.localeCompare(b.name),
  );

  const [step, setStep] = useState(0);
  const [rows, setRows] = useState<Record<string, AddRow>>(() =>
    Object.fromEntries(items.map((s) => [s.stockItemId, { qty: "", unit: s.unit, paid: "" }])),
  );
  const [checked, setChecked] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const update = (id: string, patch: Partial<AddRow>) => setRows((r) => ({ ...r, [id]: { ...r[id], ...patch } }));

  const parsed = items.map((s) => {
    const row = rows[s.stockItemId];
    const result = toStockLine(s, row, currency);
    const qty = parseCount(row.qty) ?? 0;
    const added = round3(convertQuantity(qty, row.unit, s.unit));
    return { s, row, ...result, added, after: round3(s.quantity + added) };
  });
  const lines = parsed.flatMap((p) => (p.line ? [p.line] : []));
  const invalid = parsed.some((p) => p.error !== null);
  const totalPaid = lines.reduce((t, l) => t + (l.totalCost ?? 0), 0);
  const empty = parsed.filter((p) => p.after === 0 && p.s.active);
  const canSuggest = items.some((s) => s.active && (suggested[s.stockItemId] ?? 0) > 0);

  function fillSuggested() {
    setRows((r) => {
      const next = { ...r };
      for (const s of items) {
        const qty = suggested[s.stockItemId] ?? 0;
        if (qty > 0 && s.active && next[s.stockItemId].qty === "") next[s.stockItemId] = { ...next[s.stockItemId], qty: String(qty), unit: s.unit };
      }
      return next;
    });
  }

  function review() {
    setChecked(true);
    if (invalid) return;
    setStep(1);
    window.scrollTo({ top: 0 });
  }

  async function submit() {
    if (pending) return;
    setPending(true);
    setError(null);
    try {
      await openSession({ stock: lines });
      router.replace("/dashboard");
      router.refresh();
    } catch (e) {
      setPending(false);
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      if (e instanceof ApiError && e.errorCode === "SESSION_ALREADY_OPEN") {
        router.replace("/dashboard");
        return router.refresh();
      }
      if (!(e instanceof ApiError)) console.error(e);
      setError(e instanceof ApiError ? e.message : errorMessage(""));
    }
  }

  return (
    <div className="mt-6">
      <ol aria-label={t("steps")} className="flex items-center gap-2 rounded-box border border-base-300 bg-white/60 px-4 py-4 text-sm sm:gap-3 sm:px-6">
        {steps.map((label, i) => (
          <li key={label} aria-current={i === step ? "step" : undefined} className={`flex items-center gap-2 sm:gap-3 ${i > 0 ? "flex-1" : ""}`}>
            {i > 0 && <span aria-hidden className={`h-px flex-1 ${i <= step ? "bg-base-content" : "bg-base-300"}`} />}
            <span
              className={`grid size-7 shrink-0 place-items-center rounded-full text-xs ${
                i < step ? "bg-base-content text-base-100" : i === step ? "border-2 border-base-content font-semibold" : "bg-base-200 text-base-content/70"
              }`}
            >
              {i < step ? <LuCheck className="size-3.5" aria-label={t("done")} /> : i + 1}
            </span>
            <span className={`whitespace-nowrap ${i === step ? "font-semibold" : "text-base-content/70 max-sm:sr-only"}`}>{label}</span>
          </li>
        ))}
      </ol>

      {step === 0 ? (
        <section aria-labelledby="stock-heading" className="mt-6 rounded-box border border-base-300 bg-white/60">
          <div className="flex flex-wrap items-start justify-between gap-4 px-6 py-5">
            <div>
              <h2 id="stock-heading" className="text-xl font-semibold">{t("stockTitle")}</h2>
              <p className="mt-1 max-w-2xl text-sm text-base-content/70">
                {t("stockLead")}
              </p>
            </div>
            {canSuggest && (
              <button type="button" className="btn btn-sm border-base-300 bg-white font-normal" onClick={fillSuggested}>
                {t("fillSuggested")}
              </button>
            )}
          </div>
          {/* Di bawah sm tiap baris jadi kartu dua kolom (label kecil menggantikan kepala tabel), supaya isian tidak perlu digeser. */}
          <div className="relative overflow-x-auto max-sm:border-t max-sm:border-base-300">
            <table className="w-full text-left text-sm max-sm:block sm:min-w-4xl">
              <thead className="max-sm:hidden border-y border-base-300 bg-base-200/40 text-xs uppercase tracking-widest text-base-content/70">
                <tr>
                  <th className="py-3 pr-3 pl-6 font-normal">{t("colIngredient")}</th>
                  <th className="px-3 py-3 font-normal">{t("colInStock")}</th>
                  <th className="px-3 py-3 font-normal">{t("colSuggested")}</th>
                  <th className="px-3 py-3 font-normal">{t("colAdd")}</th>
                  <th className="px-3 py-3 font-normal">{t("colPaid")}</th>
                  <th className="py-3 pr-6 pl-3 text-right font-normal">{t("colAfter")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-base-300 max-sm:block">
                {parsed.map(({ s, row, line, error: rowError, added, after }) => {
                  const id = s.stockItemId;
                  const shown = checked ? rowError : null;
                  const units = purchaseUnitsFor(s.unit);
                  const sug = suggested[id] ?? 0;
                  const perUnit = line?.totalCost && added > 0 ? line.totalCost / added : null;
                  return (
                    <tr key={id} className={`align-top max-sm:grid max-sm:grid-cols-2 max-sm:gap-x-3 max-sm:gap-y-3 max-sm:px-4 max-sm:py-4 ${added > 0 ? "bg-base-200/40" : ""}`}>
                      <td className="py-4 pr-3 pl-6 max-sm:col-span-2 max-sm:p-0 max-sm:font-semibold">
                        {s.name}
                        {s.isPrimary && <span className="ml-2 whitespace-nowrap rounded-full bg-base-200 px-2 py-0.5 text-xs">{t("mainItem")}</span>}
                        {!s.active && <span className="mt-0.5 block text-xs text-base-content/60">{t("inactiveLeftovers")}</span>}
                      </td>
                      <td className="whitespace-nowrap px-3 py-4 max-sm:p-0">
                        <span aria-hidden className="mb-1 block text-xs font-normal text-base-content/60 sm:hidden">{t("colInStock")}</span>
                        <span className={s.quantity > 0 ? "font-semibold" : "text-base-content/50"}>{f.quantity(s.quantity, s.unit)}</span>
                        {s.ageDays > 1 && <span className="block text-xs text-base-content/60">{t("daysOld", { count: s.ageDays })}</span>}
                      </td>
                      <td className="whitespace-nowrap px-3 py-4 text-base-content/70 max-sm:p-0">
                        <span aria-hidden className="mb-1 block text-xs font-normal text-base-content/60 sm:hidden">{t("colSuggested")}</span>
                        {sug > 0 ? f.quantity(sug, s.unit) : "–"}
                      </td>
                      {s.active ? (
                        <>
                          <td className="px-3 py-3 max-sm:p-0">
                            <span aria-hidden className="mb-1 block text-xs font-normal text-base-content/60 sm:hidden">{t("colAdd")}</span>
                            <div className="flex w-36 items-center rounded-field border border-base-300 bg-white has-aria-invalid:border-error max-sm:w-full">
                              <label className="sr-only" htmlFor={`qty-${id}`}>{t("amountAdded", { name: s.name })}</label>
                              <input
                                id={`qty-${id}`}
                                inputMode="decimal"
                                autoComplete="off"
                                placeholder="0"
                                aria-invalid={shown?.field === "qty" ? true : undefined}
                                className="w-full min-w-0 bg-transparent py-1.5 pl-3 outline-none"
                                value={row.qty}
                                onChange={(e) => update(id, { qty: e.target.value })}
                              />
                              {units.length > 1 ? (
                                <SelectMenu
                                  label={t("unitFor", { name: s.name })}
                                  value={row.unit}
                                  options={units.map((u) => ({ value: u, label: stockUnits[u].short }))}
                                  onChange={(unit) => update(id, { unit })}
                                />
                              ) : (
                                <span className="shrink-0 pr-3 text-xs text-base-content/60">{stockUnits[s.unit].short}</span>
                              )}
                            </div>
                            {shown?.field === "qty" && <p className="mt-1 max-w-36 text-xs text-error">{t(shown.message)}</p>}
                          </td>
                          <td className="px-3 py-3 max-sm:p-0">
                            <>
                                <span aria-hidden className="mb-1 block text-xs font-normal text-base-content/60 sm:hidden">{t("colPaid")}</span>
                                <div className="max-sm:w-full flex w-36 items-center rounded-field border border-base-300 bg-white has-aria-invalid:border-error">
                                  <span className="pl-3 text-xs text-base-content/60">{currencies[currency].symbol}</span>
                                  <label className="sr-only" htmlFor={`paid-${id}`}>{t("paidFor", { name: s.name })}</label>
                                  <input
                                    id={`paid-${id}`}
                                    {...moneyInputProps(currency)}
                                    aria-invalid={shown?.field === "paid" ? true : undefined}
                                    className="w-full min-w-0 bg-transparent py-1.5 pr-3 pl-1.5 outline-none"
                                    value={row.paid}
                                    onChange={(e) => update(id, { paid: typeMoney(e, currency) })}
                                  />
                                </div>
                                {shown?.field === "paid" && <p className="mt-1 max-w-36 text-xs text-error">{t(shown.message)}</p>}
                                {perUnit !== null && (
                                  <p className="mt-1 text-xs text-base-content/60">{money(Math.round(perUnit))} / {stockUnits[s.unit].short}</p>
                                )}
                            </>
                          </td>
                        </>
                      ) : (
                        <td colSpan={2} className="px-3 py-4 text-xs text-base-content/60 max-sm:col-span-2 max-sm:p-0">{t("reactivateToAdd")}</td>
                      )}
                      <td className={`whitespace-nowrap py-4 pr-6 pl-3 text-right max-sm:col-span-2 max-sm:p-0 max-sm:text-left ${after > 0 ? "font-semibold" : "text-base-content/50"}`}>
                        <span aria-hidden className="mb-1 block text-xs font-normal text-base-content/60 sm:hidden">{t("colAfter")}</span>
                        {f.quantity(after, s.unit)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-4 border-t border-base-300 px-6 py-4">
            <p className="text-sm text-base-content/70" aria-live="polite">
              {t.rich("addedSummary", {
                count: lines.length,
                amount: money(totalPaid),
                strong: (chunks) => <span className="font-semibold text-base-content">{chunks}</span>,
              })}
              {checked && invalid && <span className="ml-2 text-error" role="alert">{t("fixFirst")}</span>}
            </p>
            <button type="button" className="btn btn-primary" onClick={review}>
              {t("review")} <LuArrowRight aria-hidden className="size-4" />
            </button>
          </div>
        </section>
      ) : (
        <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[1fr_20rem]">
          <section aria-labelledby="review-heading" className="min-w-0 rounded-box border border-base-300 bg-white/60">
            <div className="px-6 py-5">
              <h2 id="review-heading" className="text-xl font-semibold">{t("openingStock")}</h2>
              <p className="mt-1 text-sm text-base-content/70">{t("openingLead")}</p>
            </div>
            <div className="relative overflow-x-auto">
              <table className="w-full min-w-xl text-left text-sm">
                <thead className="border-y border-base-300 bg-base-200/40 text-xs uppercase tracking-widest text-base-content/70">
                  <tr>
                    <th className="py-3 pr-3 pl-6 font-normal">{t("colIngredient")}</th>
                    <th className="px-3 py-3 text-right font-normal">{t("colInStock")}</th>
                    <th className="px-3 py-3 text-right font-normal">{t("colAdded")}</th>
                    <th className="px-3 py-3 text-right font-normal">{t("colOpening")}</th>
                    <th className="py-3 pr-6 pl-3 text-right font-normal">{t("colPaid")}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-base-300">
                  {parsed
                    .filter((p) => p.after > 0)
                    .map(({ s, line, added, after }) => (
                      <tr key={s.stockItemId}>
                        <td className="py-3 pr-3 pl-6">{s.name}</td>
                        <td className="whitespace-nowrap px-3 py-3 text-right text-base-content/70">{s.quantity > 0 ? f.quantity(s.quantity, s.unit) : "–"}</td>
                        <td className="whitespace-nowrap px-3 py-3 text-right">{added > 0 ? `+${f.quantity(added, s.unit)}` : "–"}</td>
                        <td className="whitespace-nowrap px-3 py-3 text-right font-semibold">{f.quantity(after, s.unit)}</td>
                        <td className="whitespace-nowrap py-3 pr-6 pl-3 text-right">
                          {line ? money(line.totalCost ?? 0) : "–"}
                        </td>
                      </tr>
                    ))}
                  {parsed.every((p) => p.after === 0) && (
                    <tr>
                      <td colSpan={5} className="px-6 py-6 text-center text-base-content/60">{t("nothingInStock")}</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            {empty.length > 0 && (
              <p className="border-t border-base-300 px-6 py-3 text-xs text-base-content/70">
                {t("startingAtZero", { names: empty.map((p) => p.s.name).join(", ") })}
              </p>
            )}
          </section>

          <section aria-labelledby="open-summary" className="h-fit rounded-box border border-base-300 bg-white/60 p-6">
            <h2 id="open-summary" className="text-xl font-semibold">{t("readyToOpen")}</h2>
            <p className="mt-3 text-3xl font-semibold tracking-tight">{money(totalPaid)}</p>
            <p className="text-sm text-base-content/70">
              {t("spentMorning", { count: lines.length })}
            </p>
            {error && <p className="mt-4 text-sm text-error" role="alert">{error}</p>}
            <button type="button" className="btn btn-primary mt-5 w-full" disabled={pending} onClick={submit}>
              {pending && <span aria-hidden className="loading loading-spinner loading-xs" />}
              {pending ? t("opening") : t("openStall")}
            </button>
            <button type="button" className="btn btn-ghost mt-2 w-full font-normal" disabled={pending} onClick={() => setStep(0)}>
              <LuArrowLeft aria-hidden className="size-4" /> {t("backToEdit")}
            </button>
            <p className="mt-3 text-xs text-base-content/60">
              {t("openNote")}
            </p>
          </section>
        </div>
      )}
    </div>
  );
}
