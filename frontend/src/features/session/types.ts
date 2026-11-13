// Kontrak modul session dan saran belanja (backend dto/session, dto/insight/RecommendationResponse).
import type { StockUnit } from "@/features/catalog/types";
import type { Id, Instant, LocalDate, LocalTime, Money, Quantity } from "@/lib/api-types";

export type SessionStatus = "OPEN" | "CLOSED";
export type CloseType = "MANUAL" | "AUTO";

/** Satu baris stok per bahan di sesi. Kolom biaya null untuk Cashier dan sebelum ditutup. */
export type SessionStock = {
  stockItemId: Id;
  name: string;
  unit: StockUnit;
  isPrimary: boolean;
  openingQty: Quantity;
  openingAgeDays: number;
  addedQty: Quantity;
  soldQty: Quantity;
  adjustedQty: Quantity;
  availableQty: Quantity;
  recommendedQty: Quantity | null;
  soldOutAt: Instant | null;
  closingQty: Quantity | null;
  carriedOverQty: Quantity | null;
  discardedQty: Quantity | null;
  soldCost: Money | null;
  discardedCost: Money | null;
  adjustmentCost: Money | null;
  carriedValue: Money | null;
};

/**
 * GET /sessions/current 🧾 → Session | null
 * POST /sessions/open 🧾 → 201 Session
 * POST /sessions/{id}/stock 🧾 → Session
 * POST /sessions/{id}/close 🧾 → Session (CLOSED, MANUAL)
 * GET /sessions/{id} (OWNER) → Session
 */
export type Session = {
  id: Id;
  businessDate: LocalDate;
  status: SessionStatus;
  openedAt: Instant;
  closedAt: Instant | null;
  closeType: CloseType | null;
  plannedCloseTime: LocalTime;
  /** Jam tutup + 30 menit; setelah itu sesi ditutup otomatis. */
  autoCloseAt: Instant;
  notes: string | null;
  stock: SessionStock[];
};

/** Satu baris belanja/produksi. Wajib totalCost > 0 atau noCost: true, tidak keduanya. */
export type StockLine = {
  stockItemId: Id;
  quantity: Quantity;
  /** Satuan pasar sekeluarga (mis. KG→GRAM, TRAY→BUTIR); server mengonversi. */
  unit?: StockUnit;
  totalCost?: Money;
  noCost?: boolean;
};

/** GET /sessions/opening-stock 🧾 → OpeningStock[]: stok awal bila lapak dibuka sekarang, semua bahan aktif plus sisa bahan nonaktif. */
export type OpeningStock = {
  stockItemId: Id;
  name: string;
  unit: StockUnit;
  isPrimary: boolean;
  /** false = bahan nonaktif yang masih punya sisa; tidak bisa ditambah. */
  active: boolean;
  /** Sisa yang dibawa dari tutup terakhir; 0 bila tidak ada. */
  quantity: Quantity;
  /** Umur sisa (hari) saat sesi dibuka; 0 bila tidak ada sisa. */
  ageDays: number;
};

/** POST /sessions/open (body, seluruhnya opsional). */
export type OpenSessionRequest = { stock?: StockLine[] };

/** POST /sessions/{id}/stock (body). */
export type AddStockRequest = { items: StockLine[] };

/** POST /sessions/{id}/close (body). Semua bahan sesi wajib ada; carryOverQty + discardQty == closingQty. */
export type CloseSessionRequest = {
  items: { stockItemId: Id; closingQty: Quantity; carryOverQty: Quantity; discardQty: Quantity }[];
  notes?: string;
};

/** Stok bahan sesudah penjualan atau void. */
export type StockLevel = {
  stockItemId: Id;
  name: string;
  availableQty: Quantity;
  soldOutAt: Instant | null;
};

// --- Saran belanja ---

export type ForecastMethod = "COLD_START" | "BASELINE" | "ML";

export type RecommendationItem = {
  stockItemId: Id;
  name: string;
  unit: StockUnit;
  requiredQty: Quantity;
  carryOverQty: Quantity;
  recommendedQty: Quantity;
  lowerQty: Quantity;
  upperQty: Quantity;
  /** null untuk Cashier. */
  estimatedCost: Money | null;
  /** code mis. WEEKDAY_UPLIFT, WEEKDAY_LOWER, STOCKOUT_CORRECTION, LOW_DATA. */
  reasons: { code: string; text: string }[];
};

/** GET /recommendations?date= 🧾 (date opsional = hari ini). 404 NOT_FOUND bila belum ada saran. */
export type Recommendation = {
  targetDate: LocalDate;
  method: ForecastMethod;
  dataPoints: number;
  lowData: boolean;
  items: RecommendationItem[];
  products: { productId: Id; name: string; predictedQty: Quantity; lowerQty: Quantity; upperQty: Quantity }[];
  generatedAt: Instant;
};
