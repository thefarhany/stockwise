import type en from "../../messages/en.json";

export type ValidationKey = keyof typeof en.validation;
/** Penerjemah pesan validasi (useTranslations("validation")); skema form dibuat dengan fungsi ini. */
export type ValidationT = (key: ValidationKey, values?: Record<string, string | number>) => string;
