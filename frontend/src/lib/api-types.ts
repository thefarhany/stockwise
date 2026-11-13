// Amplop respons bersama Core API (docs/05-api-contract.md §1, backend common/ApiResponse).
// Base URL Core API: /api/v1. Browser memanggilnya lewat BFF Next.js, bukan langsung.

/** UUID dari backend. */
export type Id = string;
/** Waktu UTC ISO 8601, mis. "2026-11-01T07:05:00Z". */
export type Instant = string;
/** Tanggal dagang lokal usaha, "YYYY-MM-DD". */
export type LocalDate = string;
/** Jam lokal usaha. Backend mengirim "HH:mm:ss" ("11:00:00"); request boleh "HH:mm". */
export type LocalTime = string;
/** Bilangan bulat dalam satuan terkecil mata uang usaha (rupiah, sen); lihat lib/currency. */
export type Money = number;
/** Kuantitas desimal dalam satuan stock item (BigDecimal di backend). */
export type Quantity = number;

export type ApiSuccess<T> = {
  success: true;
  message: string;
  data: T;
};

/** Daftar berhalaman: `data` berupa array, info halaman di amplop. */
export type ApiPage<T> = ApiSuccess<T[]> & {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ErrorCode =
  // 400
  | "VALIDATION_ERROR"
  | "COST_REQUIRED"
  // 401
  | "UNAUTHORIZED"
  | "TOKEN_EXPIRED"
  // 403
  | "FORBIDDEN"
  // 404
  | "NOT_FOUND"
  | "SESSION_NOT_FOUND"
  | "STOCK_ITEM_NOT_FOUND"
  | "PRODUCT_NOT_FOUND"
  | "SALE_NOT_FOUND"
  // 409
  | "EMAIL_TAKEN"
  | "DUPLICATE_NAME"
  | "STOCK_ITEM_IN_USE"
  | "SETUP_REQUIRED"
  | "SESSION_ALREADY_OPEN"
  | "SESSION_DATE_EXISTS"
  | "SESSION_CUTOFF_PASSED"
  | "GEOCODING_UNAVAILABLE"
  | "GEOCODING_FAILED"
  | "CURRENCY_LOCKED"
  | "SESSION_NOT_OPEN"
  | "SESSION_NOT_CLOSED"
  | "SALE_ALREADY_VOID"
  | "DATA_CONFLICT"
  // 422
  | "INSUFFICIENT_STOCK"
  | "RECIPE_REQUIRED"
  | "CARRY_OVER_NOT_ALLOWED"
  | "UNIT_MISMATCH"
  | "SESSION_CUTOFF_PASSED"
  | "CASH_INSUFFICIENT"
  // 429
  | "TOO_MANY_REQUESTS"
  | "INSIGHT_LIMIT_REACHED"
  // 500, 503
  | "INTERNAL_ERROR"
  | "INSIGHT_BUSY";

/**
 * Isi `details` yang dikirim backend, per errorCode:
 * - VALIDATION_ERROR: { fields: { [field]: pesan } }
 * - TOO_MANY_REQUESTS: { retryAfterSeconds }
 * - SETUP_REQUIRED: { missing: ("profile" | "stockItems" | "products" | "recipes")[] }
 * - SESSION_ALREADY_OPEN: { openSessionId }
 * - STOCK_ITEM_IN_USE: { products: nama produk[] }
 * - INSUFFICIENT_STOCK: { shortages: { stockItemId, name, needed, available }[] }
 * - RECIPE_REQUIRED: { productId }
 * - CASH_INSUFFICIENT: { totalAmount, cashReceived }
 * - UNIT_MISMATCH: { stockItemId, itemUnit }
 * - COST_REQUIRED, STOCK_ITEM_NOT_FOUND: { stockItemId }
 */
export type ApiErrorDetails = {
  fields?: Record<string, string>;
  retryAfterSeconds?: number;
  missing?: ("profile" | "stockItems" | "products" | "recipes")[];
  openSessionId?: Id;
  products?: string[];
  shortages?: { stockItemId: Id; name: string; needed: Quantity; available: Quantity }[];
  productId?: Id;
  totalAmount?: Money;
  cashReceived?: Money;
  stockItemId?: Id;
  itemUnit?: string;
};

export type ApiFailure = {
  success: false;
  /** Teks untuk ditampilkan. Perilaku client ditentukan dari errorCode, bukan dari message. */
  message: string;
  errorCode: ErrorCode;
  data: null;
  details?: ApiErrorDetails;
  timestamp: Instant;
};

export type ApiResponse<T> = ApiSuccess<T> | ApiFailure;
