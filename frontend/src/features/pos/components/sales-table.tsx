"use client";

import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { useRef, useState } from "react";
import { LuRotateCcw, LuX } from "react-icons/lu";
import { useMoney } from "@/components/currency";
import { getSale, voidSale } from "@/features/pos/api/sales";
import type { SaleDetail, SaleSummary } from "@/features/pos/types";
import { ApiError, errorMessage } from "@/lib/api-client";
import { formatClock } from "@/lib/format";
import { ReceiptView } from "./receipt";

const VOID_REASONS = ["wrongProduct", "cancelled", "wrongQty"] as const;
const MAX_REASON = 200;

const receiptNo = (n: number) => `#${String(n).padStart(3, "0")}`;

function StatusChip({ voided }: { voided: boolean }) {
  const t = useTranslations("sales");
  return voided ? (
    <span className="inline-flex items-center gap-2 text-sm text-error">
      <span aria-hidden className="size-2 rounded-full bg-error" /> {t("voided")}
    </span>
  ) : (
    <span className="inline-flex items-center gap-2 rounded-full bg-base-200 px-2.5 py-0.5 text-xs">
      <span aria-hidden className="size-1.5 rounded-full bg-success" /> {t("completed")}
    </span>
  );
}

/** Tabel riwayat transaksi; klik baris membuka struk, dan dari sana transaksi bisa dibatalkan. */
export function SalesTable({
  rows,
  timeZone,
  canVoid,
}: {
  rows: SaleSummary[];
  timeZone: string;
  /** Void hanya bisa selama sesi tanggal itu masih buka. */
  canVoid: boolean;
}) {
  const money = useMoney();
  const t = useTranslations("sales");
  const tc = useTranslations("common");
  const tp = useTranslations("payment");
  const locale = useLocale();
  const router = useRouter();
  const dialog = useRef<HTMLDialogElement>(null);
  // Transaksi yang terakhir dibuka; jawaban lambat untuk baris sebelumnya diabaikan.
  const openedId = useRef<string | null>(null);
  const [selected, setSelected] = useState<SaleSummary | null>(null);
  const [detail, setDetail] = useState<SaleDetail | null>(null);
  const [view, setView] = useState<"receipt" | "void">("receipt");
  const [reason, setReason] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function fail(e: unknown) {
    if (e instanceof ApiError && e.status === 401) return router.replace("/login");
    // Hanya pesan ApiError yang ditulis untuk pengguna; error lain (bug) cukup ke console.
    if (!(e instanceof ApiError)) console.error(e);
    setError(e instanceof ApiError ? e.message : errorMessage(""));
  }

  async function open(row: SaleSummary) {
    openedId.current = row.id;
    setSelected(row);
    setDetail(null);
    setView("receipt");
    setReason("");
    setError(null);
    dialog.current?.showModal();
    try {
      const d = await getSale(row.id);
      if (openedId.current === row.id) setDetail(d);
    } catch (e) {
      if (openedId.current === row.id) fail(e);
    }
  }

  async function confirmVoid() {
    if (!detail || pending || reason.trim() === "") return;
    setPending(true);
    setError(null);
    try {
      setDetail(await voidSale(detail.id, reason.trim()));
      setView("receipt");
      router.refresh();
    } catch (e) {
      fail(e);
      // Mis. lapak sudah tutup atau transaksi sudah dibatalkan orang lain: muat ulang tabelnya.
      router.refresh();
    } finally {
      setPending(false);
    }
  }

  const r = detail?.receipt;
  const voided = detail?.status === "VOID";
  const soldDate = selected
    ? new Date(selected.soldAt).toLocaleDateString(locale === "en" ? "en-GB" : locale, { day: "numeric", month: "short", weekday: "short", timeZone })
    : "";

  return (
    <>
      <div className="relative mt-6 overflow-x-auto rounded-box border border-base-300 bg-white/60">
        <table className="w-full min-w-3xl text-left text-sm">
          <thead className="text-xs uppercase tracking-widest text-base-content/70">
            <tr className="border-b border-base-300">
              <th className="px-5 py-3 font-normal">{t("colNo")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colTime")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colCashier")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colPortions")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colPayment")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colTotal")}</th>
              <th className="px-5 py-3 text-center font-normal">{t("colStatus")}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-base-300">
            {rows.map((s) => {
              const isVoid = s.status === "VOID";
              return (
                <tr
                  key={s.id}
                  onClick={() => open(s)}
                  className={`cursor-pointer hover:bg-base-200/70 ${isVoid ? "text-base-content/60" : ""}`}
                >
                  <td className="px-5 py-3 font-semibold">
                    {/* Tombol supaya baris bisa dibuka dengan keyboard; klik baris memakai handler yang sama. */}
                    <button type="button" className="hover:underline" aria-label={t("openTransaction", { no: receiptNo(s.receiptNo) })}>
                      {receiptNo(s.receiptNo)}
                    </button>
                  </td>
                  <td className="px-3 py-3 text-center">{formatClock(s.soldAt, timeZone)}</td>
                  <td className="px-3 py-3 text-center">{s.cashierName}</td>
                  <td className="px-3 py-3 text-center">{s.itemCount}</td>
                  <td className="px-3 py-3 text-center">{tp(s.paymentMethod)}</td>
                  <td className={`px-3 py-3 text-center font-semibold ${isVoid ? "line-through" : ""}`}>
                    {money(s.totalAmount)}
                  </td>
                  <td className="px-5 py-3 text-center"><StatusChip voided={isVoid} /></td>
                </tr>
              );
            })}
            {rows.length === 0 && (
              <tr>
                <td colSpan={7} className="px-5 py-10 text-center text-base-content/70">{t("none")}</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <dialog ref={dialog} className="modal" aria-labelledby="sale-title" onClose={() => setSelected(null)}>
        <div className="modal-box max-w-md p-0">
          {selected && view === "receipt" && (
            <>
              <div className="flex items-start justify-between gap-4 border-b border-base-300 px-6 py-5">
                <div>
                  <h2 id="sale-title" className="text-xl font-semibold">{t("transaction", { no: receiptNo(selected.receiptNo) })}</h2>
                  <p className="text-xs text-base-content/70">
                    {t("meta", { date: soldDate, time: formatClock(selected.soldAt, timeZone), name: selected.cashierName })}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  {detail && <StatusChip voided={voided} />}
                  <form method="dialog">
                    <button className="btn btn-ghost btn-sm btn-square" aria-label={tc("close")}><LuX className="size-5" aria-hidden /></button>
                  </form>
                </div>
              </div>

              <div className="px-6 py-5">
                {!r && !error && <p className="py-10 text-center text-sm text-base-content/70">{t("loading")}</p>}
                {r && (
                  // Struk dari riwayat selalu cetak ulang.
                  <ReceiptView receipt={r} timeZone={timeZone} label={voided ? "VOID" : "COPY"} voidReason={voided ? detail.voidReason : null} />
                )}
                {error && <p className="mt-3 text-sm text-error" role="alert">{error}</p>}
              </div>

              <div className="flex items-center justify-between gap-4 border-t border-base-300 px-6 py-4">
                {canVoid && detail && !voided ? (
                  <button type="button" className="text-sm text-error hover:underline" onClick={() => { setView("void"); setError(null); }}>
                    {t("voidTransaction")}
                  </button>
                ) : (
                  <span />
                )}
                <button type="button" className="btn btn-primary" disabled={!r} onClick={() => window.print()}>
                  {t("printReceipt")}
                </button>
              </div>
            </>
          )}

          {selected && view === "void" && r && (
            <>
              <div className="flex items-start gap-4 border-b border-base-300 px-6 py-5">
                <span className="grid size-10 shrink-0 place-items-center rounded-full bg-base-200 text-error">
                  <LuRotateCcw className="size-5" aria-hidden />
                </span>
                <div className="flex-1">
                  <h2 id="sale-title" className="text-xl font-semibold">{t("voidTitle", { no: receiptNo(r.receiptNo) })}</h2>
                  <p className="mt-1 text-sm text-base-content/70">
                    {t("voidText")}
                  </p>
                </div>
                <form method="dialog">
                  <button className="btn btn-ghost btn-sm btn-square" aria-label={tc("close")}><LuX className="size-5" aria-hidden /></button>
                </form>
              </div>

              <div className="space-y-5 px-6 py-5">
                <dl className="space-y-2 rounded-box bg-base-200/60 px-4 py-3 text-sm">
                  {r.items.map((i) => (
                    <div key={i.name} className="flex justify-between gap-4">
                      <dt>{i.name} × {i.quantity}</dt>
                      <dd>{money(i.subtotal)}</dd>
                    </div>
                  ))}
                  <div className="flex justify-between gap-4 font-semibold">
                    <dt>{t("totalPayment", { method: tp(r.paymentMethod) })}</dt>
                    <dd>{money(r.totalAmount)}</dd>
                  </div>
                </dl>

                <div>
                  <label htmlFor="void-reason" className="text-sm font-semibold">
                    {t("reason")} <span className="text-xs font-normal text-base-content/70">{t("required")}</span>
                  </label>
                  <textarea
                    id="void-reason"
                    className="textarea mt-2 w-full"
                    rows={3}
                    maxLength={MAX_REASON}
                    required
                    placeholder={t("reasonPlaceholder")}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                  />
                  <div className="mt-2 flex flex-wrap gap-2">
                    {VOID_REASONS.map((k) => {
                      const v = t(`reasons.${k}`);
                      return (
                        <button
                          key={k}
                          type="button"
                          aria-pressed={reason === v}
                          className={`rounded-full border px-3 py-1 text-xs ${reason === v ? "border-base-content bg-base-content text-base-100" : "border-base-300 hover:border-base-content/40"}`}
                          onClick={() => setReason(v)}
                        >
                          {v}
                        </button>
                      );
                    })}
                  </div>
                </div>
                {error && <p className="text-sm text-error" role="alert">{error}</p>}
              </div>

              <div className="flex justify-end gap-3 border-t border-base-300 px-6 py-4">
                <button type="button" className="btn border-base-300 bg-white" onClick={() => { setView("receipt"); setError(null); }}>
                  {tc("back")}
                </button>
                <button type="button" className="btn btn-error" disabled={pending || reason.trim() === ""} onClick={confirmVoid}>
                  {pending ? t("voiding") : t("voidTransaction")}
                </button>
              </div>
            </>
          )}
        </div>
        <form method="dialog" className="modal-backdrop">
          <button aria-label={tc("close")}>{tc("close")}</button>
        </form>
      </dialog>
    </>
  );
}
