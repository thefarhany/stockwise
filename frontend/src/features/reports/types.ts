// Kontrak ringkasan malam, insight, laporan, dan ekspor (backend dto/insight, dto/report). Semua endpoint OWNER.
import type { CloseType, SessionStatus } from "@/features/session/types";
import type { Id, Instant, LocalDate, Money, Quantity } from "@/lib/api-types";

export type InsightStatus = "NONE" | "READY";

/** POST /sessions/{id}/insight (tanpa body). Maksimal 3 per sesi; model "template" bila LLM gagal. */
export type InsightResult = {
  status: InsightStatus;
  text: string | null;
  model: string | null;
  attemptsLeft: number;
  generatedAt: Instant | null;
};

/** GET /sessions/{id}/summary. 409 SESSION_NOT_CLOSED bila sesi masih terbuka. */
export type SessionSummary = {
  sessionId: Id;
  businessDate: LocalDate;
  closeType: CloseType;
  revenue: Money;
  cogs: Money;
  wasteCost: Money;
  adjustmentCost: Money;
  carriedValue: Money;
  /** Selalu 0 di R1. */
  expenseTotal: Money;
  /** revenue − cogs − wasteCost − adjustmentCost − expenseTotal. */
  netProfit: Money;
  totalItemsSold: Quantity;
  voidCount: number;
  primaryItem: {
    stockItemId: Id;
    name: string;
    soldQty: Quantity;
    soldOutAt: Instant | null;
    lostSalesEstimate: Quantity | null;
  } | null;
  insight: InsightResult;
  nextDayRecommendationDate: LocalDate | null;
};

/** GET /sessions?from=&to=&page=&size= → ApiPage<SessionListItem>. Tanpa rentang = 31 hari terakhir. */
export type SessionListItem = {
  id: Id;
  businessDate: LocalDate;
  status: SessionStatus;
  closeType: CloseType | null;
  /** Angka keuangan null untuk sesi terbuka. */
  revenue: Money | null;
  cogs: Money | null;
  wasteCost: Money | null;
  netProfit: Money | null;
  primaryItem: {
    name: string;
    soldQty: Quantity;
    soldOutAt: Instant | null;
    recommendedQty: Quantity | null;
  } | null;
};

/** GET /reports/daily?date=. 404 NOT_FOUND bila tidak ada sesi tertutup di tanggal itu. */
export type DailyReport = {
  date: LocalDate;
  revenue: Money;
  cogs: Money;
  wasteCost: Money;
  adjustmentCost: Money;
  carriedValue: Money;
  expenseTotal: Money;
  netProfit: Money;
  transactions: number;
  voidCount: number;
  byProduct: { productId: Id; name: string; qty: Quantity; revenue: Money }[];
  /** Jam menurut zona waktu usaha. */
  byHour: { hour: number; qty: Quantity }[];
  /** Kunci: CASH, QRIS, OTHER. */
  byPaymentMethod: Record<string, Money>;
};

export type RangeTotals = {
  revenue: Money;
  cogs: Money;
  wasteCost: Money;
  netProfit: Money;
  itemsSold: Quantity;
  sessions: number;
};

/**
 * GET /reports/range?from=&to=&groupBy=day|week (maks 366 hari).
 * points hanya periode yang punya sesi tertutup; untuk week, period = Senin awal minggu.
 */
export type RangeReport = {
  from: LocalDate;
  to: LocalDate;
  groupBy: "day" | "week";
  points: (RangeTotals & { period: LocalDate })[];
  total: RangeTotals;
};

// GET /exports/sales?from=&to= → berkas .xlsx (bukan JSON); error tetap memakai amplop.
