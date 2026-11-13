import { describe, expect, it } from "vitest";
import { convertQuantity, defaultQuantityUnit, formatQuantity, formatSmallQuantity, fromStockQuantity, purchaseUnitsFor, quantityUnitsFor, toStockQuantity, unitLabel } from "./units";

describe("quantity units", () => {
  it("offers units of the same family that keep 3 decimals in the stock unit", () => {
    expect(quantityUnitsFor("KG")).toEqual(["GRAM", "KG"]);
    expect(quantityUnitsFor("GRAM")).toEqual(["GRAM", "KG"]);
    expect(quantityUnitsFor("LITER")).toEqual(["ML", "LITER"]);
    expect(quantityUnitsFor("PCS")).toEqual(["PCS", "TRAY"]);
    expect(quantityUnitsFor("BOTOL")).toEqual(["BOTOL"]);
  });
  it("defaults recipes to the small unit", () => {
    expect(defaultQuantityUnit("KG")).toBe("GRAM");
    expect(defaultQuantityUnit("LITER")).toBe("ML");
    expect(defaultQuantityUnit("TRAY")).toBe("PCS");
    expect(defaultQuantityUnit("PCS")).toBe("PCS");
    expect(defaultQuantityUnit("RENCENG")).toBe("RENCENG");
  });
  it("converts to the stock unit exactly, or null when too precise", () => {
    expect(toStockQuantity(60, "GRAM", "KG")).toBe(0.06);
    expect(toStockQuantity(250, "ML", "LITER")).toBe(0.25);
    expect(toStockQuantity(1.5, "KG", "GRAM")).toBe(1500);
    expect(toStockQuantity(0.5, "GRAM", "KG")).toBeNull();
    expect(toStockQuantity(3, "PCS", "TRAY")).toBe(0.1);
    expect(toStockQuantity(1, "PCS", "TRAY")).toBeNull();
  });
  it("converts a saved quantity back without float noise", () => {
    expect(fromStockQuantity(0.06, "GRAM", "KG")).toBe(60);
    expect(fromStockQuantity(0.035, "GRAM", "KG")).toBe(35);
  });
});

describe("purchase units", () => {
  it("offers units the backend converts", () => {
    expect(purchaseUnitsFor("GRAM")).toEqual(["GRAM", "KG"]);
    expect(purchaseUnitsFor("PCS")).toEqual(["PCS", "TRAY"]);
    expect(purchaseUnitsFor("DUS")).toEqual(["DUS"]);
  });
  it("converts within a family", () => {
    expect(convertQuantity(2, "KG", "GRAM")).toBe(2000);
    expect(convertQuantity(2, "TRAY", "PCS")).toBe(60);
  });
});

describe("labels and formatting", () => {
  it("labels packages per language", () => {
    expect(unitLabel("BOTOL", "id")).toBe("botol");
    expect(unitLabel("BOTOL", "en")).toBe("bottle");
    expect(unitLabel("DUS", "en")).toBe("box");
    expect(unitLabel("RENCENG", "en")).toBe("strip");
  });
  it("switches to the small unit below one big unit", () => {
    expect(formatQuantity(0.9, "KG", "en")).toBe("900 g");
    expect(formatQuantity(1.25, "KG", "en")).toBe("1.25 kg");
    expect(formatQuantity(7.2, "KG", "id")).toBe("7,2 kg");
    expect(formatQuantity(0.5, "TRAY", "id")).toBe("15 pcs");
    expect(formatQuantity(0, "KG", "en")).toBe("0 kg");
    expect(formatQuantity(3, "BOTOL", "id")).toBe("3 botol");
  });
  it("always uses the small unit for comparison tables", () => {
    expect(formatSmallQuantity(0.54, "KG", "id")).toBe("540 g");
    expect(formatSmallQuantity(1.7, "LITER", "id")).toBe("1.700 ml");
    expect(formatSmallQuantity(1.7, "LITER", "en")).toBe("1,700 ml");
    expect(formatSmallQuantity(10, "PCS", "en")).toBe("10 pcs");
  });
});
