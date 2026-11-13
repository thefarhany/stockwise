import type { Metadata } from "next";
import { getLocale, getTranslations } from "next-intl/server";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("privacy");
  return { title: `${t("meta")} · Stockwise`, description: t("metaDescription") };
}

const container = "mx-auto max-w-6xl px-6";
const eyebrow = "text-xs uppercase tracking-widest";

// Isi kebijakan (messages privacy.sections) harus selalu sesuai perilaku sistem; periksa ulang setiap ada perubahan
// backend atau deploy. Urutan id sama dengan urutan bagian.
const sectionIds = ["data-we-store", "data-use", "language-model", "security", "data-separation", "your-rights", "not-available-yet", "changes-contact"];
const LAST_UPDATED = "2026-11-07";

export default async function PrivacyPage() {
  const t = await getTranslations("privacy");
  const locale = await getLocale();
  const highlights = t.raw("highlights") as { title: string; text: string }[];
  const sections = (t.raw("sections") as { title: string; lead: string; points: string[] }[]).map((s, i) => ({ ...s, id: sectionIds[i]! }));
  const updated = new Date(`${LAST_UPDATED}T00:00:00Z`).toLocaleDateString(locale === "en" ? "en-GB" : locale, {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  });
  return (
    <main>
      <section className={`${container} grid gap-8 py-16 md:grid-cols-2 md:items-end md:py-20`}>
        <div>
          <p className={eyebrow}>{t("eyebrow")}</p>
          <h1 className="mt-4 text-4xl font-semibold leading-[1.05] tracking-tight sm:text-5xl md:text-6xl">{t("title")}</h1>
        </div>
        <div>
          <p className="text-base-content/80">
            {t("lead")}
          </p>
          <p className="mt-4 text-xs text-base-content/60">
            {t.rich("lastUpdated", { date: updated, time: (chunks) => <time dateTime={LAST_UPDATED}>{chunks}</time> })}
          </p>
        </div>
      </section>

      <section className="bg-white/60">
        <ul className={`${container} grid gap-8 py-14 sm:grid-cols-2 lg:grid-cols-4`}>
          {highlights.map((h) => (
            <li key={h.title} className="border-t-2 border-base-content pt-4">
              <h2 className="font-semibold">{h.title}</h2>
              <p className="mt-2 text-xs text-base-content/70">{h.text}</p>
            </li>
          ))}
        </ul>
      </section>

      <div className={`${container} grid gap-12 py-16 md:grid-cols-[14rem_1fr] md:py-20`}>
        <nav aria-label={t("onThisPage")} className="self-start md:sticky md:top-24">
          <p className={`${eyebrow} text-[10px] text-base-content/70`}>{t("onThisPage")}</p>
          <ol className="mt-3 border-t border-base-300 text-xs">
            {sections.map((s, i) => (
              <li key={s.id} className="border-b border-base-300">
                <a href={`#${s.id}`} className="flex gap-4 py-2.5 hover:underline">
                  <span className="w-5 shrink-0 text-base-content/60">0{i + 1}</span>
                  {s.title}
                </a>
              </li>
            ))}
          </ol>
        </nav>

        <div className="border-t-2 border-base-content">
          {sections.map((s, i) => (
            <section key={s.id} id={s.id} className="grid grid-cols-1 scroll-mt-24 gap-x-8 border-b border-base-300 py-8 sm:grid-cols-[3rem_1fr]">
              <span className="text-2xl text-secondary">0{i + 1}</span>
              <div>
                <h2 className="text-2xl font-semibold tracking-tight">{s.title}</h2>
                <p className="mt-3 text-sm text-base-content/70">{s.lead}</p>
                <ul className="mt-4 space-y-2 text-sm">
                  {s.points.map((p) => (
                    <li key={p} className="flex gap-4">
                      <span aria-hidden className="text-base-content/40">—</span>
                      {p}
                    </li>
                  ))}
                </ul>
              </div>
            </section>
          ))}
        </div>
      </div>
    </main>
  );
}
