import { describe, expect, it } from "vitest";
import type { OpeningStock } from "@/features/session/types";
import { type AddRow, toStockLine } from "./open-form";

const stock = { stockItemId: "a", unit: "GRAM" } as OpeningStock;
const row = (patch: Partial<AddRow>): AddRow => ({ qty: "", unit: "GRAM", paid: "", ...patch });

describe("toStockLine", () => {
  it("skips a row with nothing added", () => {
    expect(toStockLine(stock, row({}), "IDR")).toEqual({ line: null, error: null });
  });
  it("sends bought stock, with the unit only when it differs", () => {
    expect(toStockLine(stock, row({ qty: "2", unit: "KG", paid: "50000" }), "IDR").line).toEqual({ stockItemId: "a", quantity: 2, unit: "KG", totalCost: 50000 });
    expect(toStockLine(stock, row({ qty: "1,5", paid: "3000" }), "IDR").line).toEqual({ stockItemId: "a", quantity: 1.5, unit: undefined, totalCost: 3000 });
  });
  it("reads the total paid in the outlet's currency", () => {
    expect(toStockLine(stock, row({ qty: "2", paid: "50.000" }), "IDR").line?.totalCost).toBe(50000);
    expect(toStockLine(stock, row({ qty: "2", paid: "12.50" }), "USD").line?.totalCost).toBe(1250);
  });
  it("flags a missing amount or total paid", () => {
    expect(toStockLine(stock, row({ paid: "5000" }), "IDR").error?.field).toBe("qty");
    expect(toStockLine(stock, row({ qty: "0", paid: "5000" }), "IDR").error?.field).toBe("qty");
    expect(toStockLine(stock, row({ qty: "3" }), "IDR").error?.field).toBe("paid");
  });
});
