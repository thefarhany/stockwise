import { describe, expect, it } from "vitest";
import { stockItemSchema } from "./schemas";

const t = (key: string) => key;

const item = { name: "Telur", kind: "RAW", unit: "PCS", unitCost: 2500, shelfLifeDays: 1, carryOverAllowed: true, isPrimary: true };

describe("stockItemSchema", () => {
  it("accepts a valid item and rejects fractional rupiah, NaN, and shelf life outside 1–365", () => {
    expect(stockItemSchema(t).safeParse(item).success).toBe(true);
    expect(stockItemSchema(t).safeParse({ ...item, unitCost: 2.5 }).success).toBe(false);
    expect(stockItemSchema(t).safeParse({ ...item, shelfLifeDays: Number.NaN }).success).toBe(false);
    expect(stockItemSchema(t).safeParse({ ...item, shelfLifeDays: 366 }).success).toBe(false);
  });
});
