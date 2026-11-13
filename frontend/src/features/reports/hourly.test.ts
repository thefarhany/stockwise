import { describe, expect, it } from "vitest";
import { busiestHours, hourlySeries } from "./hourly";

describe("hourlySeries", () => {
  it("fills empty hours between opening and closing with 0", () => {
    expect(hourlySeries([{ hour: 9, qty: 4 }], 7, 10)).toEqual([
      { hour: 7, qty: 0 },
      { hour: 8, qty: 0 },
      { hour: 9, qty: 4 },
      { hour: 10, qty: 0 },
    ]);
  });

  it("keeps sales outside business hours", () => {
    expect(hourlySeries([{ hour: 21, qty: 1 }], 19, 20).map((h) => h.hour)).toEqual([19, 20, 21]);
  });
});

describe("busiestHours", () => {
  it("returns the two biggest hours, earlier hour first on a tie", () => {
    const s = [
      { hour: 12, qty: 5 },
      { hour: 13, qty: 20 },
      { hour: 17, qty: 19 },
      { hour: 18, qty: 19 },
    ];
    expect(busiestHours(s)).toEqual([
      { hour: 13, qty: 20 },
      { hour: 17, qty: 19 },
    ]);
  });

  it("ignores hours without sales", () => {
    expect(busiestHours([{ hour: 7, qty: 0 }])).toEqual([]);
  });
});
