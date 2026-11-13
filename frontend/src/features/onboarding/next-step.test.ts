import { describe, expect, it } from "vitest";
import { nextOnboardingStep } from "./next-step";

const done = { profileDone: true, stockItems: 1, products: 1, recipes: 1, readyToOpen: true };

describe("nextOnboardingStep", () => {
  it("returns the first unfinished step, or null when ready", () => {
    expect(nextOnboardingStep({ ...done, profileDone: false, stockItems: 0 })).toBe("/onboarding");
    expect(nextOnboardingStep({ ...done, stockItems: 0, products: 0 })).toBe("/onboarding/ingredients");
    expect(nextOnboardingStep({ ...done, products: 0, recipes: 0 })).toBe("/onboarding/products");
    expect(nextOnboardingStep({ ...done, recipes: 0 })).toBe("/onboarding/products");
    expect(nextOnboardingStep(done)).toBeNull();
  });
});
