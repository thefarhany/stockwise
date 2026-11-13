import { getLocale } from "next-intl/server";
import { bindFormat, type Format } from "./format";

/** useFormat() untuk server component async. */
export async function getFormat(): Promise<Format> {
  return bindFormat(await getLocale());
}
