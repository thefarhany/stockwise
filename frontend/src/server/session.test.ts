import { describe, expect, it } from "vitest";
import { expiresSoon } from "./session";

const jwt = (payload: object) => `x.${Buffer.from(JSON.stringify(payload)).toString("base64url")}.y`;
const now = 1_800_000_000_000;

describe("expiresSoon", () => {
  it("is false while more than the margin is left", () => {
    expect(expiresSoon(jwt({ exp: now / 1000 + 600 }), 60, now)).toBe(false);
  });

  it("is true when expired or inside the margin", () => {
    expect(expiresSoon(jwt({ exp: now / 1000 + 30 }), 60, now)).toBe(true);
    expect(expiresSoon(jwt({ exp: now / 1000 - 1 }), 60, now)).toBe(true);
  });

  it("treats a malformed token as expired", () => {
    expect(expiresSoon("not-a-jwt", 60, now)).toBe(true);
    expect(expiresSoon(jwt({ sub: "x" }), 60, now)).toBe(true);
  });
});
