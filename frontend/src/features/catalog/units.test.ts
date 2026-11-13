import { describe, expect, it } from "vitest";
import { convertQuantity, defaultQuantityUnit, purchaseUnitsFor, formatSmallQuantity, fromStockQuantity, quantityUnitsFor, toStockQuantity } from "./units";

describe("quantity units", () => {
  it("offers units of the same family that keep 3 decimals in the stock unit", () => {
    expect(quantityUnitsFor("KG")).toEqual(["GRAM", "ONS", "KG"]);
    expect(quantityUnitsFor("GRAM")).toEqual(["MG", "GRAM", "ONS", "KG"]);
    expect(quantityUnitsFor("LITER")).toEqual(["ML", "SDT", "SDM", "CUP", "LITER"]);
    expect(quantityUnitsFor("PCS")).toEqual(["PCS", "BUTIR", "BUAH", "LUSIN", "KODI", "TRAY"]);
    expect(quantityUnitsFor("ONS")).toEqual(["GRAM", "ONS", "KG"]);
    expect(quantityUnitsFor("BOTOL")).toEqual(["BOTOL"]);
  });
  it("defaults recipes to the small unit", () => {
    expect(defaultQuantityUnit("KG")).toBe("GRAM");
    expect(defaultQuantityUnit("LITER")).toBe("ML");
    expect(defaultQuantityUnit("PCS")).toBe("PCS");
    expect(defaultQuantityUnit("ONS")).toBe("GRAM");
    expect(defaultQuantityUnit("CUP")).toBe("CUP");
  });
  it("converts to the stock unit exactly, or null when too precise", () => {
    expect(toStockQuantity(60, "GRAM", "KG")).toBe(0.06);
    expect(toStockQuantity(250, "ML", "LITER")).toBe(0.25);
    expect(toStockQuantity(5, "MG", "GRAM")).toBe(0.005);
    expect(toStockQuantity(1.5, "KG", "GRAM")).toBe(1500);
    expect(toStockQuantity(0.5, "GRAM", "KG")).toBeNull();
    expect(toStockQuantity(50, "GRAM", "ONS")).toBe(0.5);
    expect(toStockQuantity(2, "SDM", "ML")).toBe(30);
    expect(toStockQuantity(3, "BUTIR", "TRAY")).toBe(0.1);
    expect(toStockQuantity(1, "BUTIR", "TRAY")).toBeNull();
  });
  it("converts a saved quantity back without float noise", () => {
    expect(fromStockQuantity(0.06, "GRAM", "KG")).toBe(60);
    expect(fromStockQuantity(0.035, "GRAM", "KG")).toBe(35);
  });
});

describe("formatSmallQuantity", () => {
  it("shows kg in grams and liters in ml, other units unchanged", () => {
    expect(formatSmallQuantity(0.54, "KG", "id")).toBe("540 g");
    expect(formatSmallQuantity(0.045, "KG", "id")).toBe("45 g");
    expect(formatSmallQuantity(1.7, "LITER", "id")).toBe("1.700 ml");
    expect(formatSmallQuantity(1.7, "LITER", "en")).toBe("1,700 ml");
    expect(formatSmallQuantity(10, "PCS", "en")).toBe("10 pcs");
  });
});

describe("purchase units", () => {
  it("offers units the backend converts, without mg", () => {
    expect(purchaseUnitsFor("GRAM")).toEqual(["GRAM", "ONS", "KG"]);
    expect(purchaseUnitsFor("BUTIR")).toEqual(["PCS", "BUTIR", "BUAH", "LUSIN", "KODI", "TRAY"]);
    expect(purchaseUnitsFor("BOTOL")).toEqual(["BOTOL"]);
  });
  it("converts within a family", () => {
    expect(convertQuantity(2, "KG", "GRAM")).toBe(2000);
    expect(convertQuantity(2, "TRAY", "BUTIR")).toBe(60);
  });
});
