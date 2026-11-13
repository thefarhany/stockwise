import { describe, expect, it } from "vitest";
import { editMoney, formatMoney, parseMoney } from "./currency";

describe("formatMoney", () => {
  it("formats the smallest unit with the currency's symbol and decimals", () => {
    expect(formatMoney(15000, "IDR")).toBe("Rp15.000");
    expect(formatMoney(1500, "USD")).toBe("$15.00");
    expect(formatMoney(123456, "EUR")).toBe("€1,234.56");
    expect(formatMoney(5, "SGD")).toBe("S$0.05");
    expect(formatMoney(0, "MYR")).toBe("RM0.00");
  });
});

describe("parseMoney", () => {
  it("reads rupiah from digits only, so formatted and pasted text both work", () => {
    expect(parseMoney("15.000", "IDR")).toBe(15000);
    expect(parseMoney("Rp 2.500", "IDR")).toBe(2500);
    expect(parseMoney("12345678901234567", "IDR")).toBe(1_234_567_890_123);
  });
  it("reads decimal currencies into cents", () => {
    expect(parseMoney("15", "USD")).toBe(1500);
    expect(parseMoney("15.5", "USD")).toBe(1550);
    expect(parseMoney("1,234.56", "EUR")).toBe(123456);
    expect(parseMoney("0.059", "USD")).toBe(5); // desimal ketiga diabaikan
    expect(parseMoney(".5", "USD")).toBe(50);
  });
  it("treats empty input as NaN", () => {
    expect(parseMoney("", "IDR")).toBeNaN();
    expect(parseMoney("abc", "USD")).toBeNaN();
  });
});

describe("editMoney", () => {
  const type = (raw: string, caret = raw.length, c: "IDR" | "USD" = "USD") => editMoney(raw, caret, c);
  it("formats while typing and keeps the caret in the whole part", () => {
    expect(type("1")).toEqual({ text: "1.00", caret: 1 });
    expect(type("14.00", 2)).toEqual({ text: "14.00", caret: 2 });
    expect(type("140.00", 3)).toEqual({ text: "140.00", caret: 3 });
    expect(type("1400.00", 4)).toEqual({ text: "1,400.00", caret: 5 });
    expect(type("15000", 5, "IDR")).toEqual({ text: "15.000", caret: 6 });
  });
  it("moves into cents on the separator and overwrites cent digits", () => {
    expect(type("14..00", 3)).toEqual({ text: "14.00", caret: 3 });
    expect(type("14.500", 4)).toEqual({ text: "14.50", caret: 4 });
    expect(type("14.5500", 5)).toEqual({ text: "14.55", caret: 5 });
  });
  it("keeps the caret on the same digit when grouping changes", () => {
    expect(type("1,00.00", 1)).toEqual({ text: "100.00", caret: 1 }); // hapus satu digit di 1,000.00
    expect(type("", 0)).toEqual({ text: "", caret: 0 });
    expect(type(".00", 0)).toEqual({ text: "", caret: 0 }); // backspace di "1.00" mengosongkan isian
    expect(type(".50", 0)).toEqual({ text: "0.50", caret: 1 });
  });
});
