import { describe, expect, it } from "vitest";
import en from "../../messages/en.json";
import id from "../../messages/id.json";
import { errorMessages } from "@/lib/error-messages";

/** Semua jalur kunci (dan panjang array) dalam satu file pesan, supaya kedua bahasa bisa dibandingkan. */
function shape(value: unknown, path = ""): string[] {
  if (Array.isArray(value)) return [`${path}[${value.length}]`, ...value.flatMap((v, i) => shape(v, `${path}[${i}]`))];
  if (value && typeof value === "object") return Object.entries(value).flatMap(([k, v]) => shape(v, path ? `${path}.${k}` : k));
  return [path];
}

/** Variabel ICU di tingkat atas pesan, mis. {name} dan {count, plural, ...} → name, count. */
const variables = (text: string) => [...text.matchAll(/\{(\w+)[,}]/g)].map((m) => m[1]).sort();

function leaves(value: unknown, path = ""): [string, string][] {
  if (typeof value === "string") return [[path, value]];
  if (value && typeof value === "object") return Object.entries(value).flatMap(([k, v]) => leaves(v, `${path}.${k}`));
  return [];
}

describe("messages", () => {
  it("has the same keys in English and Indonesian", () => {
    expect(shape(id)).toEqual(shape(en));
  });

  it("uses the same variables in both languages", () => {
    const idLeaves = new Map(leaves(id));
    for (const [path, text] of leaves(en)) expect(variables(idLeaves.get(path) ?? ""), path).toEqual(variables(text));
  });

  it("has every error message in both languages", () => {
    expect(Object.keys(errorMessages.id).sort()).toEqual(Object.keys(errorMessages.en).sort());
  });
});
