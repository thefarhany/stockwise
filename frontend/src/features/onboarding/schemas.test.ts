import { describe, expect, it } from "vitest";
import { profileSchema } from "./schemas";

const t = (key: string) => key;

const profile = { name: "Roti Bu Sari", currency: "IDR", openTime: "07:00", closeTime: "20:00", latitude: null, longitude: null, address: { full: "", street: "", village: "", district: "", city: "", province: "", postalCode: "" }, paydayDays: [25, 1], recommendationBufferPct: 5, targetMarginPct: 40 };

describe("profileSchema", () => {
  it("accepts a valid profile and rejects closing at or before opening", () => {
    expect(profileSchema(t).safeParse(profile).success).toBe(true);
    const res = profileSchema(t).safeParse({ ...profile, closeTime: "07:00" });
    expect(res.error?.issues[0].path).toEqual(["closeTime"]);
  });
  it("rejects a margin above 90 and an empty time", () => {
    expect(profileSchema(t).safeParse({ ...profile, targetMarginPct: 91 }).success).toBe(false);
    expect(profileSchema(t).safeParse({ ...profile, openTime: "" }).success).toBe(false);
  });
});
