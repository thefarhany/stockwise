import { getTranslations } from "next-intl/server";

const eyebrow = "text-xs uppercase tracking-widest";

// Urutan id sama dengan urutan messages docs.sections. Isinya harus mengikuti label dan perilaku aplikasi; periksa
// ulang setiap kali alur atau teks tombol berubah.
const sectionIds = ["sign-up", "setup", "open", "sell", "void", "close", "summary", "reports", "catalog", "staff"];

type Section = { title: string; lead: string; owner: boolean; steps: string[]; tips: string[] };

/** Teks panduan menandai nama tombol/menu dengan **...**; dirender tebal supaya mudah dicari di layar. */
function Rich({ text }: { text: string }) {
  return text.split(/\*\*(.+?)\*\*/).map((part, i) => (i % 2 ? <strong key={i}>{part}</strong> : part));
}

/**
 * Isi panduan (daftar isi + bagian + FAQ), dipakai halaman publik /docs dan halaman /guide di dalam aplikasi.
 * ownerSections=false menyembunyikan bagian khusus pemilik (untuk kasir). tocTop = offset sticky daftar isi.
 */
export async function Guide({ ownerSections = true, tocTop }: { ownerSections?: boolean; tocTop: string }) {
  const t = await getTranslations("docs");
  const sections = (t.raw("sections") as Section[])
    .map((s, i) => ({ ...s, id: sectionIds[i]! }))
    .filter((s) => ownerSections || !s.owner);
  const faq = t.raw("faq") as { q: string; a: string }[];
  const toc = [...sections, { id: "faq", title: t("faqTitle") }];
  const num = (i: number) => String(i + 1).padStart(2, "0");
  return (
    <div className="grid grid-cols-1 gap-12 md:grid-cols-[14rem_1fr]">
      <nav aria-label={t("onThisPage")} className={`self-start md:sticky ${tocTop}`}>
        <p className={`${eyebrow} text-[10px] text-base-content/70`}>{t("onThisPage")}</p>
        <ol className="mt-3 border-t border-base-300 text-xs">
          {toc.map((s, i) => (
            <li key={s.id} className="border-b border-base-300">
              <a href={`#${s.id}`} className="flex gap-4 py-2.5 hover:underline">
                <span className="w-5 shrink-0 text-base-content/60">{num(i)}</span>
                {s.title}
              </a>
            </li>
          ))}
        </ol>
      </nav>

      <div className="min-w-0 border-t-2 border-base-content">
        {sections.map((s, i) => (
          <section key={s.id} id={s.id} className="grid grid-cols-1 scroll-mt-24 gap-x-8 border-b border-base-300 py-8 sm:grid-cols-[3rem_1fr]">
            <span className="text-2xl text-secondary">{num(i)}</span>
            <div>
              <h2 className="flex flex-wrap items-center gap-3 text-2xl font-semibold tracking-tight">
                {s.title}
                {s.owner && (
                  <span className="rounded-full border border-base-300 bg-white px-2.5 py-0.5 text-xs font-normal tracking-normal">{t("ownerOnly")}</span>
                )}
              </h2>
              <p className="mt-3 text-sm text-base-content/70"><Rich text={s.lead} /></p>
              <h3 className={`${eyebrow} mt-6 text-[10px] text-base-content/70`}>{t("stepsLabel")}</h3>
              <ol className="mt-3 space-y-3 text-sm">
                {s.steps.map((step, n) => (
                  <li key={step} className="flex gap-3">
                    <span aria-hidden className="grid size-6 shrink-0 place-items-center rounded-full bg-base-200 text-xs font-semibold">{n + 1}</span>
                    <span className="pt-0.5"><Rich text={step} /></span>
                  </li>
                ))}
              </ol>
              <div className="mt-6 rounded-box bg-white/60 p-4">
                <h3 className={`${eyebrow} text-[10px] text-base-content/70`}>{t("tipsLabel")}</h3>
                <ul className="mt-2 space-y-2 text-sm">
                  {s.tips.map((tip) => (
                    <li key={tip} className="flex gap-4">
                      <span aria-hidden className="text-base-content/40">—</span>
                      <span><Rich text={tip} /></span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </section>
        ))}

        <section id="faq" className="grid grid-cols-1 scroll-mt-24 gap-x-8 py-8 sm:grid-cols-[3rem_1fr]">
          <span className="text-2xl text-secondary">{num(toc.length - 1)}</span>
          <div>
            <h2 className="text-2xl font-semibold tracking-tight">{t("faqTitle")}</h2>
            <div className="mt-4 border-t border-base-300">
              {faq.map((f) => (
                <details key={f.q} className="group border-b border-base-300 text-sm">
                  <summary className="flex cursor-pointer list-none justify-between gap-4 py-3 font-medium">
                    {f.q}
                    <span aria-hidden className="text-base-content/50 transition group-open:rotate-45">+</span>
                  </summary>
                  <p className="pb-4 text-base-content/70">{f.a}</p>
                </details>
              ))}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
