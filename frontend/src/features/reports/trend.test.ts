import { describe, expect, it } from "vitest";
import { rangeStart, trendRange } from "./trend";

const today = "2026-11-12";

describe("trendRange", () => {
  it("defaults to the last 30 days, grouped by day", () => {
    expect(trendRange({}, today)).toEqual({ from: "2026-10-14", to: today, groupBy: "day" });
  });

  it("keeps a valid range and weekly grouping", () => {
    expect(trendRange({ from: "2026-09-01", to: "2026-09-30", groupBy: "week" }, today)).toEqual({
      from: "2026-09-01",
      to: "2026-09-30",
      groupBy: "week",
    });
  });

  it("clamps the end to today and ignores impossible dates", () => {
    expect(trendRange({ from: "2026-02-31", to: "2027-01-01" }, today)).toEqual({ from: "2026-10-14", to: today, groupBy: "day" });
  });

  it("replaces a start after the end", () => {
    expect(trendRange({ from: "2026-11-10", to: "2026-11-01" }, today).from).toBe("2026-10-03");
  });

  it("caps the range at 366 days", () => {
    const r = trendRange({ from: "2020-01-01", to: today }, today);
    expect(r.from).toBe(rangeStart(today, 366));
    expect((Date.parse(r.to) - Date.parse(r.from)) / 86_400_000 + 1).toBe(366);
  });
});
