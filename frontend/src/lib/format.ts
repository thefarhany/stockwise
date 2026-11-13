// Fungsi yang bergantung bahasa menerima `locale` ("en" | "id"); di komponen pakai useFormat()/getFormat() (i18n/format).

/** "2026-11-05" → "Thursday, 5 November" / "Kamis, 5 November". Tanggal dagang tidak punya zona, jadi dibaca sebagai UTC. */
export const formatBusinessDate = (date: string, locale: string) =>
  new Date(`${date}T00:00:00Z`).toLocaleDateString(locale === "en" ? "en-GB" : locale, {
    weekday: "long",
    day: "numeric",
    month: "long",
    timeZone: "UTC",
  });

/** "2026-11-05" → "Thursday" / "Kamis". */
export const formatWeekday = (date: string, locale: string) =>
  new Date(`${date}T00:00:00Z`).toLocaleDateString(locale === "en" ? "en-GB" : locale, { weekday: "long", timeZone: "UTC" });

/** Instant UTC → "17:20" di zona waktu usaha (sama di semua bahasa). */
export const formatClock = (instant: string, timeZone: string) =>
  new Date(instant).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit", hourCycle: "h23", timeZone });

/** "11:00:00" → "11:00". */
export const hhmm = (time: string | null) => time?.slice(0, 5) ?? "–";

/** Tanggal dagang hari ini di zona waktu usaha, "YYYY-MM-DD". */
export const todayIn = (timeZone: string) => new Date().toLocaleDateString("en-CA", { timeZone });

/** "2026-11-05" → "2026-11-04". */
export const dayBefore = (date: string) =>
  new Date(Date.parse(`${date}T00:00:00Z`) - 86_400_000).toISOString().slice(0, 10);
