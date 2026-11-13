import { apiGet, apiPost } from "@/lib/api-client";
import type { CreateSaleRequest, RecapRequest, Sale, SaleDetail } from "@/features/pos/types";

export const createSale = (input: CreateSaleRequest) => apiPost<Sale>("/sales", input);

export const recordRecap = (input: RecapRequest) => apiPost<Sale>("/sales/recap", input);

export const getSale = (saleId: string) => apiGet<SaleDetail>(`/sales/${encodeURIComponent(saleId)}`);

export const voidSale = (saleId: string, reason: string) =>
  apiPost<SaleDetail>(`/sales/${encodeURIComponent(saleId)}/void`, { reason });
