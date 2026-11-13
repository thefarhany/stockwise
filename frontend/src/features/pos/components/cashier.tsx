"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useEffect, useMemo, useRef, useState } from "react";
import { LuCheck, LuMinus, LuPlus } from "react-icons/lu";
import { useCurrency, useMoney } from "@/components/currency";
import { Modal } from "@/components/ui/modal";
import { MoneyInput } from "@/components/ui/money-field";
import type { Product } from "@/features/catalog/types";
import { createSale, getSale, recordRecap } from "@/features/pos/api/sales";
import { quickCashOptions } from "@/features/pos/cash";
import type { PaymentMethod, Receipt, Sale } from "@/features/pos/types";
import type { SessionStock } from "@/features/session/types";
import { useFormat } from "@/i18n/format";
import { ApiError, errorMessage } from "@/lib/api-client";
import { formatClock } from "@/lib/format";
import { ReceiptView } from "./receipt";

type Mode = "quick" | "recap";

const payments: PaymentMethod[] = ["CASH", "QRIS", "OTHER"];

const segment = (on: boolean) =>
  `flex-1 rounded-field px-4 py-1.5 text-sm transition ${on ? "bg-white shadow-sm" : "text-base-content/70 hover:text-base-content"}`;
const chip = (on: boolean) =>
  `rounded-full border px-4 py-1.5 text-sm ${on ? "border-base-content bg-base-content text-base-100" : "border-base-300 bg-white/60 hover:border-base-content/40"}`;

/** Layar kasir: daftar produk di kiri, keranjang dan pembayaran di kanan. */
export function Cashier({
  sessionId,
  products,
  stock,
  nextReceiptNo,
  timeZone,
  dateLabel,
}: {
  sessionId: string;
  products: Product[];
  stock: SessionStock[];
  nextReceiptNo: number;
  timeZone: string;
  /** Tanggal dagang sesi, mis. "Thu, 5 Nov". */
  dateLabel: string;
}) {
  const money = useMoney();
  const currency = useCurrency();
  const t = useTranslations("cashier");
  const tp = useTranslations("payment");
  const f = useFormat();
  const router = useRouter();
  const [mode, setMode] = useState<Mode>("quick");
  const [category, setCategory] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [cart, setCart] = useState<Record<string, number>>({});
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [cash, setCash] = useState(Number.NaN);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Transaksi yang baru tersimpan; modalnya terbuka sampai kasir memulai transaksi baru.
  const [saved, setSaved] = useState<Sale | null>(null);
  // Data struk (nama usaha, nama kasir) untuk cetak; dimuat setelah transaksi tersimpan.
  const [receipt, setReceipt] = useState<Receipt | null>(null);
  const newSaleButton = useRef<HTMLButtonElement>(null);

  // Enter memulai transaksi baru selama modal sukses terbuka, juga bila fokus hilang (mis. setelah dialog cetak).
  useEffect(() => {
    if (!saved) return;
    const onKey = (e: KeyboardEvent) => {
      const onOtherButton = e.target instanceof HTMLButtonElement && e.target !== newSaleButton.current;
      if (e.key === "Enter" && !onOtherButton) {
        e.preventDefault();
        setSaved(null);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [saved]);

  const byId = useMemo(() => new Map(products.map((p) => [p.id, p])), [products]);
  const stockById = useMemo(() => new Map(stock.map((s) => [s.stockItemId, s])), [stock]);
  const categories = [...new Set(products.flatMap((p) => (p.category ? [p.category] : [])))];
  const q = query.trim().toLowerCase();
  const shown = products.filter(
    (p) => (category === null || p.category === category) && (q === "" || p.name.toLowerCase().includes(q)),
  );

  const lines = Object.entries(cart).flatMap(([id, qty]) => {
    const p = byId.get(id);
    return p ? [{ product: p, qty }] : [];
  });
  const total = lines.reduce((sum, l) => sum + l.product.price * l.qty, 0);
  const portions = lines.reduce((sum, l) => sum + l.qty, 0);
  const takesCash = mode === "quick" && method === "CASH";
  const hasCash = takesCash && Number.isFinite(cash);
  const shortCash = hasCash && cash < total;

  /** Ubah jumlah di keranjang; dibatasi porsi yang masih bisa dibuat dari stok. 0 = hapus. */
  function setQty(p: Product, qty: number) {
    const max = p.maxPortionsFromStock ?? 0;
    setError(null);
    setCart((c) => {
      const next = { ...c };
      const clamped = Math.min(Math.max(qty, 0), max);
      if (clamped === 0) delete next[p.id];
      else next[p.id] = clamped;
      return next;
    });
  }

  /** Bahan resep yang habis, mis. "Cheese sold out 13:05". */
  function soldOutReason(p: Product) {
    const empty = p.recipe.map((r) => stockById.get(r.stockItemId)).find((s) => s && s.availableQty <= 0);
    if (!empty) return null;
    return empty.soldOutAt
      ? t("soldOutAt", { name: empty.name, time: formatClock(empty.soldOutAt, timeZone) })
      : t("soldOutName", { name: empty.name });
  }

  async function pay() {
    if (lines.length === 0 || pending || shortCash) return;
    setPending(true);
    setError(null);
    const items = lines.map((l) => ({ productId: l.product.id, quantity: l.qty }));
    try {
      const sale =
        mode === "quick"
          ? await createSale({ sessionId, paymentMethod: method, cashReceived: hasCash ? cash : undefined, items })
          : await recordRecap({ sessionId, paymentMethod: method, items });
      setSaved(sale);
      setReceipt(null);
      setCart({});
      setCash(Number.NaN);
      router.refresh();
      getSale(sale.id)
        .then((d) => setReceipt(d.receipt))
        // Struk gagal dimuat tidak membatalkan transaksi; tombol cetak tetap nonaktif.
        .catch((e) => console.error(e));
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        router.replace("/login");
        return;
      }
      if (e instanceof ApiError && e.errorCode === "INSUFFICIENT_STOCK" && e.details?.shortages?.length) {
        setError(t("notEnough", { items: e.details.shortages.map((s) => s.name).join(", ") }));
      } else {
        // Hanya pesan ApiError yang ditulis untuk pengguna; error lain (bug) cukup ke console.
        if (!(e instanceof ApiError)) console.error(e);
        setError(e instanceof ApiError ? e.message : errorMessage(""));
      }
      // Stok atau status lapak mungkin sudah berubah; muat ulang angka dari server.
      router.refresh();
    } finally {
      setPending(false);
    }
  }

  const primary = stock.find((s) => s.isPrimary);
  const others = stock.filter((s) => !s.isPrimary);

  return (
    <div className="grid grid-cols-1 min-h-screen lg:grid-cols-[1fr_24rem]">
      <div className={`min-w-0 px-4 py-6 sm:px-6 sm:py-8 lg:px-10 ${portions > 0 ? "max-lg:pb-24" : ""}`}>
        <header className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-4">
            <h1 className="text-3xl font-semibold tracking-tight">{t("title")}</h1>
            <span className="flex items-center gap-2 rounded-full bg-base-200 px-2.5 py-0.5 text-xs">
              <span aria-hidden className="size-2 rounded-full bg-success" /> {t("stallOpen", { date: dateLabel })}
            </span>
          </div>
          <div role="group" aria-label={t("inputMode")} className="flex rounded-box bg-base-200 p-1">
            <button type="button" aria-pressed={mode === "quick"} className={segment(mode === "quick")} onClick={() => setMode("quick")}>
              {t("quick")}
            </button>
            <button type="button" aria-pressed={mode === "recap"} className={segment(mode === "recap")} onClick={() => setMode("recap")}>
              {t("recap")}
            </button>
          </div>
        </header>
        {mode === "recap" && (
          <p className="mt-3 text-sm text-base-content/70">
            {t("recapHint")}
          </p>
        )}

        {primary && (
          <section aria-label={t("mainItemStock")} className="mt-5 flex flex-wrap items-center gap-x-5 gap-y-2 rounded-box border border-base-300 bg-white/60 px-4 py-3 text-sm">
            <span className="text-xs uppercase tracking-widest text-base-content/70">{t("mainItem")}</span>
            <span>
              {primary.name} <strong>{f.quantity(Math.max(primary.availableQty, 0), primary.unit)}</strong>
            </span>
            <progress
              className="progress h-1.5 min-w-24 flex-1"
              value={Math.max(primary.availableQty, 0)}
              max={Math.max(primary.openingQty + primary.addedQty, 1)}
              aria-label={t("left", { name: primary.name })}
            />
            {others.length > 0 && (
              <span className="text-sm text-base-content/70">
                {others
                  .map((s) => (s.availableQty > 0 ? `${s.name} ${f.quantity(s.availableQty, s.unit)}` : t("soldOutName", { name: s.name })))
                  .join(" · ")}
              </span>
            )}
          </section>
        )}

        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <div role="group" aria-label={t("category")} className="flex flex-wrap gap-2">
            <button type="button" aria-pressed={category === null} className={chip(category === null)} onClick={() => setCategory(null)}>
              {t("all")}
            </button>
            {categories.map((c) => (
              <button key={c} type="button" aria-pressed={category === c} className={chip(category === c)} onClick={() => setCategory(c)}>
                {c}
              </button>
            ))}
          </div>
          <input
            type="search"
            aria-label={t("search")}
            placeholder={t("search")}
            className="input input-sm w-full bg-white/60 sm:w-64"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>

        <ul className="mt-5 grid grid-cols-2 gap-3 xl:grid-cols-4">
          {shown.map((p) => {
            const qty = cart[p.id] ?? 0;
            const soldOut = p.available !== true;
            const reason = soldOut ? soldOutReason(p) : null;
            return (
              <li key={p.id}>
                <button
                  type="button"
                  disabled={soldOut || qty >= (p.maxPortionsFromStock ?? 0)}
                  onClick={() => setQty(p, qty + 1)}
                  aria-label={qty > 0 ? t("addProductInCart", { name: p.name, count: qty }) : t("addProduct", { name: p.name })}
                  className={`flex h-full min-h-28 w-full flex-col rounded-box p-3 text-left text-sm transition sm:min-h-32 sm:p-4 ${
                    soldOut
                      ? "cursor-not-allowed bg-base-200/70 text-base-content/50"
                      : qty > 0
                        ? "border-2 border-base-content bg-white"
                        : "border border-base-300 bg-white/60 hover:border-base-content/40"
                  }`}
                >
                  <span className="flex items-start justify-between gap-2">
                    <span className="font-semibold">{p.name}</span>
                    {qty > 0 && (
                      <span className="grid size-6 shrink-0 place-items-center rounded-full bg-base-content text-xs text-base-100">{qty}</span>
                    )}
                  </span>
                  <span className="mt-0.5 text-xs text-base-content/60">
                    {[p.category, reason].filter(Boolean).join(" · ")}
                  </span>
                  <span className="mt-auto flex items-end justify-between gap-2 pt-4">
                    <span className="text-base">{money(p.price)}</span>
                    {soldOut ? (
                      <span className="flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full bg-white px-2 py-0.5 text-xs text-error/80">
                        <span aria-hidden className="size-1.5 rounded-full bg-error/70" /> {t("soldOut")}
                      </span>
                    ) : (
                      <span className="shrink-0 whitespace-nowrap text-xs text-base-content/60">{t("leftPortions", { count: p.maxPortionsFromStock ?? 0 })}</span>
                    )}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
        {shown.length === 0 && <p className="mt-5 text-sm text-base-content/70">{t("noMatch")}</p>}
      </div>

      <aside id="cart" aria-labelledby="cart-heading" className="flex scroll-mt-4 flex-col border-t border-base-300 bg-white/50 lg:sticky lg:top-0 lg:h-screen lg:border-t-0 lg:border-l">
        <div className="flex items-start justify-between gap-4 border-b border-base-300 px-4 py-5 sm:px-6">
          <div>
            <h2 id="cart-heading" className="text-xl font-semibold">{t("cart")}</h2>
            <p className="text-xs text-base-content/70">{t("nextTransaction", { no: String(nextReceiptNo).padStart(3, "0") })}</p>
          </div>
          {lines.length > 0 && (
            <button type="button" className="text-sm hover:underline" onClick={() => setCart({})}>
              {t("clear")}
            </button>
          )}
        </div>

        <ul className="flex-1 divide-y divide-base-300 overflow-y-auto px-4 text-sm sm:px-6">
          {lines.map(({ product: p, qty }) => (
            <li key={p.id} className="flex items-center gap-3 py-3">
              <span className="min-w-0 flex-1">
                <span className="block truncate">{p.name}</span>
                <span className="text-xs text-base-content/70">{t("perPortion", { price: money(p.price) })}</span>
              </span>
              <span className="flex items-center rounded-field border border-base-300">
                <button type="button" aria-label={t("removeOne", { name: p.name })} className="btn btn-ghost btn-xs btn-square" onClick={() => setQty(p, qty - 1)}>
                  <LuMinus aria-hidden />
                </button>
                <span className="w-7 text-center" aria-label={`${qty} ${p.name}`}>{qty}</span>
                <button
                  type="button"
                  aria-label={t("addOne", { name: p.name })}
                  disabled={qty >= (p.maxPortionsFromStock ?? 0)}
                  className="btn btn-ghost btn-xs btn-square"
                  onClick={() => setQty(p, qty + 1)}
                >
                  <LuPlus aria-hidden />
                </button>
              </span>
              <span className="w-20 text-right font-semibold">{money(p.price * qty)}</span>
            </li>
          ))}
          {lines.length === 0 && (
            <li className="py-8 text-center text-xs text-base-content/70">{t("tapToAdd")}</li>
          )}
        </ul>

        <div className={`space-y-4 border-t border-base-300 px-4 py-5 sm:px-6 ${portions > 0 ? "max-lg:pb-24" : ""}`}>
          <p className="flex items-end justify-between gap-4">
            <span className="text-sm text-base-content/80">{t("totalPortions", { count: portions })}</span>
            <span className="text-3xl font-semibold tracking-tight">{money(total)}</span>
          </p>

          <div role="group" aria-label={t("paymentMethod")} className="flex rounded-box bg-base-200 p-1">
            {payments.map((m) => (
              <button key={m} type="button" aria-pressed={method === m} className={segment(method === m)} onClick={() => setMethod(m)}>
                {tp(m)}
              </button>
            ))}
          </div>

          {takesCash && (
            <div className="space-y-3">
              <MoneyInput
                id="cash-received"
                label={t("cashReceived")}
                value={cash}
                onValueChange={setCash}
                error={shortCash ? t("lessThanTotal") : undefined}
              />
              {total > 0 && (
                <div className="grid grid-cols-3 gap-2">
                  {[total, ...quickCashOptions(total, currency)].map((v, i) => (
                    <button key={v} type="button" aria-pressed={cash === v} className={chip(cash === v)} onClick={() => setCash(v)}>
                      {i === 0 ? t("exact") : money(v)}
                    </button>
                  ))}
                </div>
              )}
              {hasCash && !shortCash && (
                <p className="flex items-center justify-between rounded-box bg-base-200 px-4 py-3 text-sm">
                  <span>{t("change")}</span>
                  <span className="text-xl font-semibold">{money(cash - total)}</span>
                </p>
              )}
            </div>
          )}

          <div aria-live="polite">
            {error && <p className="text-sm text-error">{error}</p>}
          </div>

          <button type="button" className="btn btn-primary w-full" disabled={lines.length === 0 || pending || shortCash} onClick={pay}>
            {pending ? t("saving") : mode === "quick" ? t("pay", { amount: money(total) }) : t("saveRecap", { amount: money(total) })}
          </button>
        </div>
      </aside>

      {/* HP/tablet: keranjang ada di bawah daftar produk, jadi total dan pintasan ke keranjang selalu terlihat. */}
      {portions > 0 && (
        <a
          href="#cart"
          className="fixed inset-x-4 bottom-4 z-20 flex items-center justify-between gap-4 rounded-box bg-neutral px-5 py-3.5 text-neutral-content shadow-lg lg:hidden"
        >
          <span className="text-sm">{t("totalPortions", { count: portions })}</span>
          <span className="flex items-center gap-2 font-semibold">
            {money(total)}
            <span className="text-sm font-normal">· {t("viewCart")}</span>
          </span>
        </a>
      )}

      <Modal open={saved !== null} onClose={() => setSaved(null)} labelledBy="sale-saved-title" className="max-w-sm">
        {saved && (
          <div className="px-7 py-8 text-center">
            <span className="mx-auto grid size-14 place-items-center rounded-full bg-base-content text-base-100">
              <LuCheck className="size-6" aria-hidden />
            </span>
            <h2 id="sale-saved-title" className="mt-5 text-xl font-semibold">
              {t("saved", { no: String(saved.receiptNo).padStart(3, "0") })}
            </h2>
            <div className="mt-5 rounded-box bg-base-200 px-5 py-5">
              {saved.change !== null ? (
                <>
                  <p className="text-xs uppercase tracking-widest text-base-content/70">{t("change")}</p>
                  <p className="mt-1 text-4xl font-semibold tracking-tight">{money(saved.change)}</p>
                  <p className="mt-2 text-sm text-base-content/80">
                    {t("totalCash", { total: money(saved.totalAmount), cash: money(saved.cashReceived ?? 0) })}
                  </p>
                </>
              ) : (
                <>
                  <p className="text-xs uppercase tracking-widest text-base-content/70">{t("totalPaid")}</p>
                  <p className="mt-1 text-4xl font-semibold tracking-tight">{money(saved.totalAmount)}</p>
                  <p className="mt-2 text-sm text-base-content/80">{tp(saved.paymentMethod)}</p>
                </>
              )}
            </div>
            <div className="mt-6 grid grid-cols-2 gap-3">
              <button
                type="button"
                className="btn border-base-300 bg-white"
                disabled={!receipt}
                onClick={() => {
                  window.print();
                  // Setelah dialog cetak ditutup, Enter berikutnya memulai transaksi baru, bukan mencetak ulang.
                  newSaleButton.current?.focus();
                }}
              >
                {t("printReceipt")}
              </button>
              {/* Fokus awal di sini: Enter langsung memulai transaksi baru. */}
              <button ref={newSaleButton} type="button" className="btn btn-primary" data-autofocus onClick={() => setSaved(null)}>
                {t("newTransaction")}
              </button>
            </div>
            <p className="mt-4 text-xs text-base-content/60">{t("enterHint")}</p>
            {/* Hanya tampil saat dicetak. */}
            {receipt && <ReceiptView receipt={receipt} timeZone={timeZone} className="hidden print:block" />}
          </div>
        )}
      </Modal>
    </div>
  );
}
