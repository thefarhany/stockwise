import { useLocale, useTranslations } from "next-intl";
import { useCurrency, useMoney } from "@/components/currency";
import type { Receipt } from "@/features/pos/types";
import { formatAmount } from "@/lib/currency";
import { formatClock } from "@/lib/format";

/**
 * Struk; elemen `.receipt-print` satu-satunya yang tercetak (lihat globals.css).
 * `label` hanya tampil saat dicetak: COPY untuk cetak ulang, VOID untuk transaksi batal.
 */
export function ReceiptView({
  receipt: r,
  timeZone,
  label,
  voidReason,
  className = "",
}: {
  receipt: Receipt;
  timeZone: string;
  label?: "COPY" | "VOID";
  voidReason?: string | null;
  className?: string;
}) {
  const money = useMoney();
  const currency = useCurrency();
  const locale = useLocale();
  const t = useTranslations("receipt");
  const tp = useTranslations("payment");
  // Harga per baris tanpa simbol mata uang, supaya struk tetap ringkas.
  const plain = (n: number) => formatAmount(n, currency);
  return (
    <div className={`receipt-print rounded-box border border-base-300 bg-base-200/40 px-5 py-4 text-left text-sm ${className}`}>
      <div className="border-b border-dashed border-base-300 pb-3 text-center">
        <p className="font-semibold">{r.businessName}</p>
        <p className="text-xs text-base-content/70">
          {t("no", { no: r.receiptNo })} · {new Date(r.soldAt).toLocaleDateString(locale === "en" ? "en-GB" : locale, { timeZone })} {formatClock(r.soldAt, timeZone)} ·{" "}
          {r.cashierName}
        </p>
        {label && <p className="hidden font-semibold print:block">{t(label)}</p>}
      </div>
      <ul className="space-y-3 border-b border-dashed border-base-300 py-3">
        {r.items.map((i) => (
          <li key={i.name} className="flex justify-between gap-4">
            <span>
              {i.name}
              <span className="block text-xs text-base-content/70">{i.quantity} × {plain(i.unitPrice)}</span>
            </span>
            <span>{plain(i.subtotal)}</span>
          </li>
        ))}
      </ul>
      <dl className="space-y-2 border-b border-dashed border-base-300 py-3">
        <div className="flex justify-between"><dt>{t("total")}</dt><dd className="font-semibold">{money(r.totalAmount)}</dd></div>
        <div className="flex justify-between">
          <dt>{tp(r.paymentMethod)}</dt>
          <dd>{money(r.cashReceived ?? r.totalAmount)}</dd>
        </div>
        {r.change !== null && (
          <div className="flex justify-between"><dt>{t("change")}</dt><dd>{money(r.change)}</dd></div>
        )}
      </dl>
      {voidReason && <p className="border-b border-dashed border-base-300 py-3 text-xs text-error">{t("voided", { reason: voidReason })}</p>}
      <p className="pt-3 text-center text-xs text-base-content/70">{t("thanks")}</p>
    </div>
  );
}
