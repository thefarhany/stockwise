// Validasi form bahan dan produk (dipakai onboarding dan menu Ingredients, Products and recipes).
// Skema dibuat per bahasa: berikan t = useTranslations("validation").
import { z } from "zod";
import type { ValidationT } from "@/i18n/validation";
import { stockUnitKeys } from "./units";

// Batas sama dengan backend dto/catalog/StockItemRequest (Money.MAX_UNIT_PRICE). Uang dalam satuan terkecil (lib/currency).
export const stockItemSchema = (t: ValidationT) =>
  z.object({
    name: z.string().trim().min(1, t("ingredientNameRequired")).max(100, t("maxChars", { max: 100 })),
    kind: z.enum(["RAW", "FINISHED"]),
    unit: z.enum(stockUnitKeys),
    unitCost: z
      .number({ error: t("priceRequired") })
      .int(t("priceRequired"))
      .min(0, t("priceNegative"))
      .max(1_000_000_000, t("priceTooLarge")),
    shelfLifeDays: z.number({ error: t("shelfLife") }).int(t("shelfLife")).min(1, t("shelfLife")).max(365, t("shelfLife")),
    carryOverAllowed: z.boolean(),
    isPrimary: z.boolean(),
  });

export type StockItemInput = z.infer<ReturnType<typeof stockItemSchema>>;

// Produk dan resepnya dibuat dalam satu langkah, supaya harga jual dipilih setelah HPP terlihat.
// Batas sama dengan backend dto/catalog/ProductRequest, RecipeRequest, dan ProductService.replaceRecipe.
export const productRecipeSchema = (t: ValidationT) =>
  z.object({
    name: z.string().trim().min(1, t("productNameRequired")).max(100, t("maxChars", { max: 100 })),
    category: z.string().trim().max(50, t("maxChars", { max: 50 })),
    items: z
      .array(
        z.object({
          stockItemId: z.string().min(1, t("chooseIngredient")),
          unit: z.enum(stockUnitKeys),
          quantity: z
            .number({ error: t("amountAboveZero") })
            .gt(0, t("amountAboveZero"))
            .max(1_000_000, t("maxAmount", { max: 1_000_000 }))
            .refine((n) => Math.abs(n * 1000 - Math.round(n * 1000)) < 1e-6, t("maxDecimals")),
        }),
      )
      .min(1, t("addIngredient"))
      .max(50, t("maxIngredients"))
      .superRefine((items, ctx) => {
        const seen = new Set<string>();
        items.forEach((item, i) => {
          if (item.stockItemId && seen.has(item.stockItemId)) {
            ctx.addIssue({ code: "custom", path: [i, "stockItemId"], message: t("duplicateRecipeIngredient") });
          }
          seen.add(item.stockItemId);
        });
      }),
    price: z
      .number({ error: t("sellingPriceRequired") })
      .int(t("sellingPriceRequired"))
      .min(0, t("priceNegative"))
      .max(1_000_000_000, t("priceTooLarge")),
  });

export type ProductRecipeInput = z.infer<ReturnType<typeof productRecipeSchema>>;
