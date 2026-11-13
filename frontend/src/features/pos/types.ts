// Kontrak modul sales: kasir, riwayat, struk, void (backend dto/sales). Semua endpoint 🧾.
import type { StockLevel } from "@/features/session/types";
import type { Id, Instant, Money } from "@/lib/api-types";

export type PaymentMethod = "CASH" | "QRIS" | "OTHER";
export type SaleStatus = "COMPLETED" | "VOID";

/** Jumlah 1–100000 per produk, produk tidak boleh dobel, maksimal 100 baris. */
export type SaleItemRequest = { productId: Id; quantity: number };

/** POST /sales (body). cashReceived hanya untuk CASH, opsional, harus ≥ total. */
export type CreateSaleRequest = {
  sessionId: Id;
  paymentMethod: PaymentMethod;
  cashReceived?: Money;
  items: SaleItemRequest[];
};

/** POST /sales/recap (body): rekap per produk, disimpan sebagai satu transaksi. */
export type RecapRequest = {
  sessionId: Id;
  paymentMethod: PaymentMethod;
  items: SaleItemRequest[];
};

export type SaleItem = {
  productId: Id;
  name: string;
  quantity: number;
  unitPrice: Money;
  subtotal: Money;
};

/** POST /sales dan POST /sales/recap → 201 Sale. */
export type Sale = {
  id: Id;
  /** Urut per sesi, mulai dari 1. */
  receiptNo: number;
  soldAt: Instant;
  status: SaleStatus;
  totalAmount: Money;
  paymentMethod: PaymentMethod;
  cashReceived: Money | null;
  /** null bila tanpa cashReceived. */
  change: Money | null;
  items: SaleItem[];
  stockAfter: StockLevel[];
};

/**
 * GET /sales?sessionId=&date=&page=&size= → ApiPage<SaleSummary>, terbaru dulu.
 * Owner: sesi/tanggal mana pun (default hari ini). Cashier: sesi berjalan saja, selain itu 403.
 */
export type SaleSummary = {
  id: Id;
  receiptNo: number;
  soldAt: Instant;
  totalAmount: Money;
  paymentMethod: PaymentMethod;
  status: SaleStatus;
  cashierName: string;
  itemCount: number;
};

/** Data struk; label SALINAN/BATAL ditambahkan client saat cetak. */
export type Receipt = {
  businessName: string;
  receiptNo: number;
  soldAt: Instant;
  cashierName: string;
  items: { name: string; quantity: number; unitPrice: Money; subtotal: Money }[];
  totalAmount: Money;
  paymentMethod: PaymentMethod;
  cashReceived: Money | null;
  change: Money | null;
};

/** GET /sales/{id} dan POST /sales/{id}/void → SaleDetail. */
export type SaleDetail = {
  id: Id;
  status: SaleStatus;
  receipt: Receipt;
  voidReason: string | null;
};

/** POST /sales/{id}/void (body). Alasan wajib, maksimal 200 karakter; hanya di sesi terbuka. */
export type VoidSaleRequest = { reason: string };
