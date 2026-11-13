// Kontrak modul catalog: bahan, produk, resep (backend dto/catalog).
// Daftar tidak berhalaman; default hanya yang aktif, `?includeInactive=true` untuk semua.
import type { Id, Money, Quantity } from "@/lib/api-types";

export type StockKind = "RAW" | "FINISHED";
// Sama dengan backend model/catalog/StockUnit.
export type StockUnit = "GRAM" | "KG" | "ML" | "LITER" | "PCS" | "TRAY" | "BOTOL" | "PACK" | "DUS" | "RENCENG";
/** BATCH ditolak di R1. */
export type StockMode = "MADE_TO_ORDER" | "BATCH";

// --- Bahan ---
// GET /stock-items 🧾 → StockItem[]
// DELETE /stock-items/{id} (OWNER) → data: null (menonaktifkan)

export type StockItem = {
  id: Id;
  name: string;
  kind: StockKind;
  unit: StockUnit;
  /** Harga perkiraan per satuan. null untuk Cashier. */
  unitCost: Money | null;
  shelfLifeDays: number;
  carryOverAllowed: boolean;
  isPrimary: boolean;
  active: boolean;
};

/** POST /stock-items → 201, PUT /stock-items/{id} → StockItem (OWNER). */
export type StockItemRequest = {
  name: string;
  kind: StockKind;
  unit: StockUnit;
  /** Default 0. */
  unitCost?: Money;
  /** 1–365, default 1. */
  shelfLifeDays?: number;
  /** Default true. */
  carryOverAllowed?: boolean;
  /** Default false. */
  isPrimary?: boolean;
  /** Hanya untuk PUT: `true` mengaktifkan kembali. */
  active?: boolean;
};

// --- Produk ---
// GET /products 🧾 → Product[]
// DELETE /products/{id} (OWNER) → data: null (menonaktifkan)

export type RecipeItem = {
  stockItemId: Id;
  stockItemName: string;
  unit: StockUnit;
  quantity: Quantity;
};

export type Product = {
  id: Id;
  name: string;
  category: string | null;
  price: Money;
  stockMode: StockMode;
  /** HPP per porsi. null untuk Cashier atau produk tanpa resep. */
  hpp: Money | null;
  /** true bila ada bahan yang masih memakai harga perkiraan. */
  hppEstimated: boolean | null;
  margin: Money | null;
  /** Saran harga jual dari HPP dan target margin (kelipatan Rp500). */
  suggestedPrice: Money | null;
  /** Dari sesi terbuka; null bila tidak ada sesi atau tanpa resep. */
  available: boolean | null;
  maxPortionsFromStock: number | null;
  active: boolean;
  recipe: RecipeItem[];
};

/** POST /products → 201, PUT /products/{id} → Product (OWNER). */
export type ProductRequest = {
  name: string;
  category?: string | null;
  price: Money;
  /** Default MADE_TO_ORDER. */
  stockMode?: StockMode;
  /** 0–32000, default 0. */
  sortOrder?: number;
  active?: boolean;
};

/** PUT /products/{id}/recipe (body, OWNER): mengganti seluruh resep, maksimal 50 baris. */
export type RecipeRequest = {
  items: { stockItemId: Id; quantity: Quantity }[];
};

/** PUT /products/{id}/recipe → RecipeResponse. */
export type RecipeResponse = { productId: Id; items: RecipeItem[] };
