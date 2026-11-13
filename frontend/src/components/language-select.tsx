"use client";

import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";
import { LuLanguages } from "react-icons/lu";
import { setLanguage } from "@/features/auth/api/auth";
import { LOCALE_COOKIE, localeNames, locales, toLanguage, type Locale } from "@/i18n/locale";
import { ApiError } from "@/lib/api-client";

const saveBrowserLocale = (locale: Locale) => {
  document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=${60 * 60 * 24 * 365}; samesite=lax`;
};

/**
 * Pilihan bahasa. `account`: disimpan di akun yang sedang login (aplikasi). `browser`: disimpan di cookie browser ini
 * (halaman publik, login, daftar). Setelah itu halaman dirender ulang di server dalam bahasa baru.
 */
export function LanguageSelect({
  scope,
  compact = false,
  className = "",
}: {
  scope: "account" | "browser";
  /** Tombol EN/ID kecil, bukan dropdown. Warnanya ikut warna teks sekitarnya (bisa di latar terang maupun gelap). */
  compact?: boolean;
  className?: string;
}) {
  const t = useTranslations("common");
  const locale = useLocale();
  const router = useRouter();
  const [pending, setPending] = useState(false);

  async function change(next: Locale) {
    setPending(true);
    try {
      if (scope === "account") await setLanguage(toLanguage(next));
      else saveBrowserLocale(next);
      router.refresh();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) router.replace("/login");
    } finally {
      setPending(false);
    }
  }

  if (compact)
    return (
      <div
        role="group"
        aria-label={t("language")}
        className={`inline-flex rounded-full border border-current/20 p-0.5 text-xs font-medium ${className}`}
      >
        {locales.map((l) => (
          <button
            key={l}
            type="button"
            lang={l}
            title={localeNames[l]}
            aria-label={localeNames[l]}
            aria-pressed={l === locale}
            disabled={pending}
            onClick={() => l !== locale && change(l)}
            className={`rounded-full px-2.5 py-1 uppercase transition-colors ${
              l === locale ? "bg-current/15" : "opacity-70 hover:opacity-100"
            }`}
          >
            {l}
          </button>
        ))}
      </div>
    );

  return (
    <label className={`flex items-center gap-2 text-sm ${className}`}>
      <LuLanguages className="size-4 shrink-0 text-base-content/70" aria-hidden />
      <span className="sr-only">{t("language")}</span>
      <select
        value={locale}
        disabled={pending}
        onChange={(e) => change(e.target.value as Locale)}
        className="select select-sm w-auto border-base-300 bg-white"
      >
        {locales.map((l) => (
          <option key={l} value={l}>{localeNames[l]}</option>
        ))}
      </select>
    </label>
  );
}
