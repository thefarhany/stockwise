"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { useMoney } from "@/components/currency";
import type { StockItem } from "@/features/catalog/types";
import { closeSession } from "@/features/session/api/sessions";
import type { SessionStock } from "@/features/session/types";
import { useFormat } from "@/i18n/format";
import { ApiError, errorMessage } from "@/lib/api-client";

type Action = "keep" | "discard";

/** Jumlah hitungan: ≥ 0, paling banyak 3 desimal (sama dengan backend). null = isian belum valid. */
export function parseCount(text: string): number | null {
  const t = text.trim().replace(",", ".");
  if (!/^\d+(\.\d{1,3})?$/.test(t)) return null;
  return Number(t);
}

/** Nilai uang utuh (satuan terkecil) dari jumlah × harga per satuan, dihitung dalam seperseribu supaya pas. */
const value = (qty: number, unitCost: number) => Math.round((Math.round(qty * 1000) * unitCost) / 1000);

const segment = (on: boolean) =>
  `rounded-field px-2.5 py-1 text-sm transition disabled:cursor-not-allowed disabled:opacity-40 ${on ? "bg-white font-semibold shadow-sm" : "text-base-content/70"}`;

/** Hitung sisa tiap bahan sesi, pilih simpan/buang, lalu tutup lapak. */
export function CloseForm({
  sessionId,
  stock,
  stockItems,
  autoCloseLabel,
}: {
  sessionId: string;
  stock: SessionStock[];
  /** Untuk harga perkiraan dan aturan "boleh dibawa". Harga null untuk Cashier. */
  stockItems: StockItem[];
  /** Mis. "20:30". */
  autoCloseLabel: string;
}) {
  const money = useMoney();
  const t = useTranslations("close");
  const f = useFormat();
  const router = useRouter();
  const byId = new Map(stockItems.map((s) => [s.id, s]));
  const rows = stock.toSorted((a, b) => Number(b.isPrimary) - Number(a.isPrimary));
  const canKeep = (s: SessionStock) => byId.get(s.stockItemId)?.carryOverAllowed ?? true;
  const recorded = (s: SessionStock) => Math.max(s.availableQty, 0);

  const [counted, setCounted] = useState<Record<string, string>>(() =>
    Object.fromEntries(rows.map((s) => [s.stockItemId, String(recorded(s))])),
  );
  const [action, setAction] = useState<Record<string, Action>>(() =>
    Object.fromEntries(rows.map((s) => [s.stockItemId, canKeep(s) ? "keep" : "discard"])),
  );
  const [notes, setNotes] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const parsed = rows.map((s) => ({ s, qty: parseCount(counted[s.stockItemId] ?? ""), unitCost: byId.get(s.stockItemId)?.unitCost ?? null }));
  const invalid = parsed.some((p) => p.qty === null);
  const showCost = parsed.some((p) => p.unitCost !== null);
  const kept = parsed.filter((p) => p.qty !== null && p.qty > 0 && action[p.s.stockItemId] === "keep");
  const discarded = parsed.filter((p) => p.qty !== null && p.qty > 0 && action[p.s.stockItemId] === "discard");
  const missing = parsed.filter((p) => p.qty !== null && p.qty < recorded(p.s));
  const keptValue = kept.reduce((t, p) => t + value(p.qty!, p.unitCost ?? 0), 0);

  async function submit() {
    if (invalid || pending) return;
    setPending(true);
    setError(null);
    try {
      await closeSession(sessionId, {
        items: parsed.map(({ s, qty }) => {
          const keep = action[s.stockItemId] === "keep";
          return { stockItemId: s.stockItemId, closingQty: qty!, carryOverQty: keep ? qty! : 0, discardQty: keep ? 0 : qty! };
        }),
        notes: notes.trim() || undefined,
      });
      router.replace("/dashboard");
      router.refresh();
    } catch (e) {
      setPending(false);
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      if (!(e instanceof ApiError)) console.error(e);
      setError(e instanceof ApiError ? e.message : errorMessage(""));
    }
  }

  return (
    <div className="mt-6 grid grid-cols-1 gap-6 xl:grid-cols-[1fr_20rem]">
      <div className="min-w-0 space-y-6">
        <section aria-labelledby="count-heading" className="overflow-hidden rounded-box border border-base-300 bg-white/60">
          <div className="flex flex-wrap items-start justify-between gap-4 px-6 py-5">
            <div>
              <h2 id="count-heading" className="text-xl font-semibold">{t("countTitle")}</h2>
              <p className="mt-1 text-sm text-base-content/70">{t("countLead")}</p>
            </div>
            <button
              type="button"
              className="btn btn-sm border-base-300 bg-white font-normal"
              onClick={() => setCounted(Object.fromEntries(rows.map((s) => [s.stockItemId, String(recorded(s))])))}
            >
              {t("useRecorded")}
            </button>
          </div>
          {/* Di bawah sm tiap baris jadi kartu dua kolom (label kecil menggantikan kepala tabel), supaya isian tidak perlu digeser. */}
          <div className="relative overflow-x-auto max-sm:border-t max-sm:border-base-300">
            <table className="w-full text-left text-sm max-sm:block sm:min-w-2xl">
              <thead className="max-sm:hidden border-y border-base-300 bg-base-200/40 text-xs uppercase tracking-widest text-base-content/70">
                <tr>
                  <th className="py-3 pr-3 pl-6 font-normal">{t("colIngredient")}</th>
                  <th className="px-3 py-3 text-center font-normal">{t("colRecorded")}</th>
                  <th className="px-3 py-3 text-center font-normal">{t("colCounted")}</th>
                  <th className="px-3 py-3 text-center font-normal">{t("colDifference")}</th>
                  <th className="py-3 pr-6 pl-3 text-center font-normal">{t("colAction")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-base-300 max-sm:block">
                {parsed.map(({ s, qty }) => {
                  const id = s.stockItemId;
                  const diff = qty === null ? null : Math.round((qty - recorded(s)) * 1000) / 1000;
                  const keepAllowed = canKeep(s);
                  return (
                    <tr key={id} className="align-top max-sm:grid max-sm:grid-cols-2 max-sm:gap-x-3 max-sm:gap-y-3 max-sm:px-4 max-sm:py-4">
                      <td className="py-4 pr-3 pl-6 max-sm:col-span-2 max-sm:p-0 max-sm:font-semibold">
                        {s.name}
                        {s.isPrimary && <span className="ml-2 whitespace-nowrap rounded-full bg-base-200 px-2 py-0.5 text-xs">{t("mainItem")}</span>}
                      </td>
                      <td className="whitespace-nowrap px-3 py-4 sm:text-center max-sm:p-0">
                        <span aria-hidden className="mb-1 block text-xs text-base-content/60 sm:hidden">{t("colRecorded")}</span>
                        {f.quantity(recorded(s), s.unit)}
                      </td>
                      <td className="px-3 py-3 sm:text-center max-sm:p-0">
                        <span aria-hidden className="mb-1 block text-xs text-base-content/60 sm:hidden">{t("colCounted")}</span>
                        <label className="flex w-28 items-center sm:mx-auto rounded-field border border-base-300 bg-white px-3 has-[:invalid]:border-error has-aria-invalid:border-error">
                          <span className="sr-only">{t("countedName", { name: s.name })}</span>
                          <input
                            inputMode="decimal"
                            autoComplete="off"
                            aria-invalid={qty === null ? true : undefined}
                            className="w-full bg-transparent py-1.5 outline-none"
                            value={counted[id] ?? ""}
                            onChange={(e) => setCounted((c) => ({ ...c, [id]: e.target.value }))}
                          />
                          <span className="text-xs text-base-content/60">{f.unit(s.unit)}</span>
                        </label>
                        {qty === null && <p className="mt-1 text-xs text-error">{t("countInvalid")}</p>}
                        {diff !== null && diff < 0 && <p className="mt-1 text-xs text-base-content/60">{t("spoiled", { qty: f.quantity(-diff, s.unit) })}</p>}
                      </td>
                      <td className={`px-3 py-4 sm:text-center max-sm:p-0 ${diff !== null && diff !== 0 ? "font-semibold text-error" : "text-base-content/70"}`}>
                        <span aria-hidden className="mb-1 block text-xs text-base-content/60 sm:hidden">{t("colDifference")}</span>
                        {diff === null ? "–" : diff === 0 ? "0" : `${diff > 0 ? "+" : "−"}${f.quantity(Math.abs(diff), s.unit)}`}
                      </td>
                      <td className="py-3 pr-6 pl-3 sm:text-center max-sm:p-0">
                        <span aria-hidden className="mb-1 block text-xs text-base-content/60 sm:hidden">{t("colAction")}</span>
                        <div role="group" aria-label={t("actionFor", { name: s.name })} className="inline-flex rounded-field bg-base-200 p-1">
                          <button type="button" aria-pressed={action[id] === "keep"} disabled={!keepAllowed} className={segment(action[id] === "keep")} onClick={() => setAction((a) => ({ ...a, [id]: "keep" }))}>
                            {t("keep")}
                          </button>
                          <button type="button" aria-pressed={action[id] === "discard"} className={segment(action[id] === "discard")} onClick={() => setAction((a) => ({ ...a, [id]: "discard" }))}>
                            {t("discard")}
                          </button>
                        </div>
                        {!keepAllowed && <p className="mt-1 text-xs text-base-content/60">{t("cantCarry")}</p>}
                        {keepAllowed && action[id] === "discard" && <p className="mt-1 text-xs text-base-content/60">{t("chosenDiscard")}</p>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <p className="border-t border-base-300 px-6 py-3 text-xs text-base-content/70">
            {t("tableNote")}
          </p>
        </section>

        <section className="rounded-box border border-base-300 bg-white/60 px-6 py-5">
          <label htmlFor="session-notes" className="block text-sm font-semibold">{t("notes")}</label>
          <input
            id="session-notes"
            className="input mt-2 w-full"
            maxLength={1000}
            placeholder={t("notesPlaceholder")}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </section>
      </div>

      <div className="space-y-6">
        <section aria-labelledby="leftover-summary" aria-live="polite" className="rounded-box border border-base-300 bg-white/60 p-6">
          <h2 id="leftover-summary" className="text-xl font-semibold">{t("summaryTitle")}</h2>
          {showCost && (
            <>
              <p className="mt-3 text-3xl font-semibold tracking-tight">{money(keptValue)}</p>
              <p className="text-sm text-base-content/70">{t("carriedValue")}</p>
            </>
          )}
          <dl className="mt-4 space-y-2.5 text-sm">
            {kept.map((p) => (
              <div key={p.s.stockItemId} className="flex justify-between gap-3">
                <dt>{p.s.name} · {f.quantity(p.qty!, p.s.unit)}</dt>
                {showCost && <dd>{money(value(p.qty!, p.unitCost ?? 0))}</dd>}
              </div>
            ))}
            {kept.length === 0 && <p className="text-base-content/60">{t("nothingCarried")}</p>}
          </dl>
          {(discarded.length > 0 || missing.length > 0) && (
            <dl className="mt-3 space-y-2.5 border-t border-base-300 pt-3 text-sm">
              {discarded.map((p) => (
                <div key={p.s.stockItemId} className="flex justify-between gap-3">
                  <dt>{t("discardedRow", { name: p.s.name, qty: f.quantity(p.qty!, p.s.unit) })}</dt>
                  {showCost && <dd className="text-error">{money(value(p.qty!, p.unitCost ?? 0))}</dd>}
                </div>
              ))}
              {missing.map((p) => {
                const gap = Math.round((recorded(p.s) - p.qty!) * 1000) / 1000;
                return (
                  <div key={p.s.stockItemId} className="flex justify-between gap-3">
                    <dt>{t("differenceRow", { name: p.s.name, qty: f.quantity(gap, p.s.unit) })}</dt>
                    {showCost && <dd className="text-error">{money(value(gap, p.unitCost ?? 0))}</dd>}
                  </div>
                );
              })}
            </dl>
          )}
          {error && <p className="mt-4 text-sm text-error" role="alert">{error}</p>}
          <button type="button" className="btn btn-primary mt-5 w-full" disabled={invalid || pending} onClick={submit}>
            {pending ? t("closing") : t("closeStall")}
          </button>
          <p className="mt-2 text-center text-xs text-base-content/60">{t("noChanges")}</p>
          {showCost && (
            <p className="mt-3 text-xs text-base-content/60">
              {t("valuesNote")}
            </p>
          )}
        </section>

        <section className="rounded-box bg-base-200 p-6">
          <h2 className="text-lg font-semibold">{t("forgotTitle")}</h2>
          <p className="mt-2 text-sm text-base-content/80">
            {t("forgotText", { time: autoCloseLabel })}
          </p>
        </section>
      </div>
    </div>
  );
}
