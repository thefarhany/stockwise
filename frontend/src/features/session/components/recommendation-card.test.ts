import { createTranslator } from "next-intl";
import { describe, expect, it } from "vitest";
import { formatQuantity } from "@/features/catalog/units";
import type { Recommendation, RecommendationItem } from "@/features/session/types";
import { formatWeekday } from "@/lib/format";
import en from "../../../../messages/en.json";
import id from "../../../../messages/id.json";
import { parseStockout, reasonText } from "./recommendation-card";
import type { Format } from "@/i18n/format";

const item = { stockItemId: "egg", name: "Eggs", unit: "PCS" } as RecommendationItem;
const rec = { targetDate: "2026-11-05", dataPoints: 3 } as Recommendation;
const soldOut = {
  timeZone: "Asia/Jakarta",
  primary: { stockItemId: "egg", name: "Eggs", soldQty: 100, soldOutAt: "2026-11-04T10:20:00Z", lostSalesEstimate: 8.6 },
};
const tEn = createTranslator({ locale: "en", messages: en, namespace: "recommendation" });
const tId = createTranslator({ locale: "id", messages: id, namespace: "recommendation" });
const fmt = (locale: string) => ({ weekday: (d: string) => formatWeekday(d, locale) }) as Format;

describe("reasonText", () => {
  it("rebuilds backend reasons in the display language", () => {
    expect(reasonText("WEEKDAY_UPLIFT", "Kamis biasanya lebih ramai", item, rec, soldOut, tEn, fmt("en"))).toBe(
      "Thursdays are usually busier",
    );
    expect(reasonText("WEEKDAY_UPLIFT", "Kamis biasanya lebih ramai", item, rec, soldOut, tId, fmt("id"))).toBe(
      "Hari Kamis biasanya lebih ramai",
    );
    expect(reasonText("STOCKOUT_CORRECTION", "x", item, rec, soldOut, tEn, fmt("en"))).toBe(
      "Eggs sold out at 17:20, about 9 customers missed out",
    );
    expect(reasonText("LOW_DATA", "x", item, rec, soldOut, tEn, fmt("en"))).toBe("Little data so far (3 days)");
  });

  it("falls back to the backend text for unknown codes", () => {
    expect(reasonText("NEW_CODE", "Teks baru", item, rec, soldOut, tEn, fmt("en"))).toBe("Teks baru");
  });
});

describe("formatQuantity", () => {
  it("uses the language's decimal separator and the short unit", () => {
    expect(formatQuantity(7.2, "KG", "id")).toBe("7,2 kg");
    expect(formatQuantity(7.2, "KG", "en")).toBe("7.2 kg");
    expect(formatQuantity(120, "PCS", "en")).toBe("120 pcs");
  });
});

describe("parseStockout", () => {
  it("reads the sold-out time and missed customers from the backend text", () => {
    expect(parseStockout("Kemarin habis jam 17.20, sekitar 9 pembeli terlewat")).toEqual({ time: "17:20", missed: 9 });
    expect(parseStockout("Kemarin habis jam 9.05")).toEqual({ time: "09:05", missed: 0 });
    expect(parseStockout("Kamis biasanya lebih ramai")).toBeNull();
  });

  it("is used for the reason when there is no summary (cashier)", () => {
    const noSummary = { timeZone: "Asia/Jakarta", primary: null };
    expect(reasonText("STOCKOUT_CORRECTION", "Kemarin habis jam 17.20, sekitar 9 pembeli terlewat", item, rec, noSummary, tEn, fmt("en"))).toBe(
      "Eggs sold out at 17:20, about 9 customers missed out",
    );
  });
});
