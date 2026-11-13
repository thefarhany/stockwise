import { apiDelete, apiPost, apiPut } from "@/lib/api-client";
import type { Product, ProductRequest, RecipeRequest, RecipeResponse } from "@/features/catalog/types";

export const createProduct = (input: ProductRequest) => apiPost<Product>("/products", input);

export const updateProduct = (productId: string, input: ProductRequest) =>
  apiPut<Product>(`/products/${encodeURIComponent(productId)}`, input);

export const saveRecipe = (productId: string, input: RecipeRequest) =>
  apiPut<RecipeResponse>(`/products/${encodeURIComponent(productId)}/recipe`, input);

/** Menonaktifkan; produk hilang dari kasir, riwayat tetap utuh. */
export const deactivateProduct = (productId: string) => apiDelete<null>(`/products/${encodeURIComponent(productId)}`);
