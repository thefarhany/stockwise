import { describe, expect, it } from "vitest";
import { quickCashOptions } from "./cash";

describe("quickCashOptions", () => {
  it("suggests the next 50k and 100k notes above the total", () => {
    expect(quickCashOptions(41_000, "IDR")).toEqual([50_000, 100_000]);
    expect(quickCashOptions(120_000, "IDR")).toEqual([150_000, 200_000]);
  });

  it("drops an option equal to the total (covered by exact cash)", () => {
    expect(quickCashOptions(50_000, "IDR")).toEqual([100_000]);
    expect(quickCashOptions(100_000, "IDR")).toEqual([150_000, 200_000]);
  });

  it("uses the currency's own notes", () => {
    expect(quickCashOptions(1230, "USD")).toEqual([2000, 5000]); // $12.30 → $20, $50
  });

  it("returns nothing for an empty cart", () => {
    expect(quickCashOptions(0, "IDR")).toEqual([]);
  });
});
