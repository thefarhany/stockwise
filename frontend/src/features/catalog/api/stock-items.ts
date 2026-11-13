import { apiDelete, apiPost, apiPut } from "@/lib/api-client";
import type { StockItem, StockItemRequest } from "@/features/catalog/types";

const path = (id: string) => `/stock-items/${encodeURIComponent(id)}`;

export const createStockItem = (input: StockItemRequest) => apiPost<StockItem>("/stock-items", input);

/** `active: true` mengaktifkan kembali bahan yang nonaktif. */
export const updateStockItem = (id: string, input: StockItemRequest) => apiPut<StockItem>(path(id), input);

/** Menonaktifkan; ditolak STOCK_ITEM_IN_USE bila masih dipakai resep produk aktif. */
export const deactivateStockItem = (id: string) => apiDelete<null>(path(id));
