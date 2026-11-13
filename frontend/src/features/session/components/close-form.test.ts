import { describe, expect, it } from "vitest";
import { parseCount } from "./close-form";

describe("parseCount", () => {
  it("accepts 0 or more with up to 3 decimals, comma or dot", () => {
    expect(parseCount("36")).toBe(36);
    expect(parseCount("0,25")).toBe(0.25);
    expect(parseCount(" 1.125 ")).toBe(1.125);
    expect(parseCount("0")).toBe(0);
  });

  it("rejects empty, negative, and too precise input", () => {
    expect(parseCount("")).toBeNull();
    expect(parseCount("-1")).toBeNull();
    expect(parseCount("0.0001")).toBeNull();
    expect(parseCount("abc")).toBeNull();
  });
});
