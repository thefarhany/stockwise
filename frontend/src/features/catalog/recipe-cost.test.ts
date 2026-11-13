import { describe, expect, it } from "vitest";
import { lineCost, recipeCost, suggestPrice } from "./recipe-cost";

describe("recipe cost", () => {
  it("matches the design example: flour 60 g × Rp14, 1 egg × Rp2.500, chocolate 15 g × Rp60", () => {
    const lines = [{ quantity: 60, unitCost: 14 }, { quantity: 1, unitCost: 2500 }, { quantity: 15, unitCost: 60 }];
    expect(lines.map((l) => lineCost(l.quantity, l.unitCost))).toEqual([840, 2500, 900]);
    expect(recipeCost(lines)).toBe(4240);
  });
  it("rounds the total half up once, not per line", () => {
    expect(recipeCost([{ quantity: 0.5, unitCost: 1 }, { quantity: 0.5, unitCost: 1 }])).toBe(1);
    expect(recipeCost([{ quantity: 0.5, unitCost: 5 }])).toBe(3);
    expect(recipeCost([{ quantity: 0.1, unitCost: 3 }, { quantity: 0.2, unitCost: 3 }])).toBe(1);
  });
});

describe("suggestPrice", () => {
  it("follows the PRD examples and rounds up to Rp500", () => {
    expect(suggestPrice(7800, 40, "IDR")).toBe(13000);
    expect(suggestPrice(4240, 40, "IDR")).toBe(7500);
    expect(suggestPrice(1000, 0, "IDR")).toBe(1000);
  });
  it("returns null without a cost", () => {
    expect(suggestPrice(0, 40, "IDR")).toBeNull();
  });
});
