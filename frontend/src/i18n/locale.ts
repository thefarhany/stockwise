// Bahasa tampilan. Aplikasi (setelah login) memakai bahasa akun (backend app_user.language); halaman publik serta
// login/daftar memakai pilihan per browser yang disimpan di cookie.

export const locales = ["en", "id"] as const;
export type Locale = (typeof locales)[number];

/** Bahasa akun di API (backend model/auth/Language). */
export type Language = "EN" | "ID";

export const LOCALE_COOKIE = "sw_lang";
export const localeNames: Record<Locale, string> = { en: "English", id: "Bahasa Indonesia" };

export const toLocale = (language: Language): Locale => (language === "ID" ? "id" : "en");
export const toLanguage = (locale: Locale): Language => (locale === "id" ? "ID" : "EN");
export const isLocale = (value: unknown): value is Locale => locales.includes(value as Locale);

/** Halaman publik dan login/daftar: bahasanya per browser, bukan per akun. */
export const isPublicPath = (path: string) =>
  ["/", "/features", "/for-you", "/how-it-works", "/docs", "/privacy", "/login", "/signup"].includes(path.replace(/\/$/, "") || "/");

/** Tanpa pilihan tersimpan: bahasa browser bila Indonesia, selain itu English. */
export const fromAcceptLanguage = (header: string | null): Locale =>
  /^\s*(id|in)\b/i.test(header ?? "") ? "id" : "en";
