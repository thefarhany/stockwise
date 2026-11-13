// Bahasa untuk setiap request (next-intl). Header x-sw-path diisi proxy.ts.
import { cookies, headers } from "next/headers";
import { getRequestConfig } from "next-intl/server";
import { getProfile } from "@/server/profile";
import { ACCESS_COOKIE } from "@/server/session";
import { fromAcceptLanguage, isLocale, isPublicPath, LOCALE_COOKIE, toLocale, type Locale } from "./locale";

async function resolveLocale(): Promise<Locale> {
  const [h, c] = await Promise.all([headers(), cookies()]);
  if (!isPublicPath(h.get("x-sw-path") ?? "/") && c.get(ACCESS_COOKIE)) {
    const me = await getProfile();
    if (me?.success) return toLocale(me.data.user.language);
  }
  const saved = c.get(LOCALE_COOKIE)?.value;
  return isLocale(saved) ? saved : fromAcceptLanguage(h.get("accept-language"));
}

export default getRequestConfig(async () => {
  const locale = await resolveLocale();
  return { locale, messages: (await import(`../../messages/${locale}.json`)).default };
});
