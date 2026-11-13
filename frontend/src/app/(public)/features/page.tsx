import type { Metadata } from "next";
import Image from "next/image";
import { getTranslations } from "next-intl/server";
import { LuArrowRight } from "react-icons/lu";
import { CtaBand } from "../_components/cta-band";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("features");
  return { title: `${t("meta")} · Stockwise`, description: t("metaDescription") };
}

const container = "mx-auto max-w-6xl px-6";
const eyebrow = "text-xs uppercase tracking-widest";

type CardImage = { src: string; width: number; height: number };

function Card({ shaded, image, label, title, text }: { shaded: boolean; image: CardImage; label: string; title: string; text: string }) {
  return (
    <article
      className={`row-span-3 grid grid-rows-subgrid gap-y-0 rounded-box p-5 ${shaded ? "bg-base-200" : "border border-base-300 bg-white/70"}`}
    >
      {/* Tinggi gambar sama di semua kartu supaya judul dan teks sejajar; object-contain agar gambar tidak terpotong. */}
      <Image {...image} alt="" className="mx-auto h-40 w-full max-w-60 object-contain" />
      <div className="mt-5">
        <p className="text-[10px] uppercase tracking-widest text-base-content/70">{label}</p>
        <h3 className="mt-1 font-semibold">{title}</h3>
      </div>
      <p className="mt-2 text-xs text-base-content/70">{text}</p>
    </article>
  );
}

/** Ilustrasi kartu per kelompok (file di public/images), urutannya sama dengan features.groups di messages. */
const groupImages: CardImage[][] = [
  [
    { src: "/images/mobile-payments.svg", width: 720, height: 719 },
    { src: "/images/empty-shelf.svg", width: 896, height: 748 },
    { src: "/images/receipt.svg", width: 961, height: 880 },
  ],
  [
    { src: "/images/calculator.svg", width: 960, height: 608 },
    { src: "/images/groceries.svg", width: 662, height: 646 },
    { src: "/images/daily-report.svg", width: 960, height: 601 },
  ],
  [
    { src: "/images/clock.svg", width: 764, height: 809 },
    { src: "/images/team-counter.svg", width: 936, height: 505 },
    { src: "/images/secure-data.svg", width: 793, height: 552 },
  ],
];

/** Pola papan catur: putih dan abu-abu selang-seling, baris berikutnya mulai dari warna kebalikannya. */
const isShaded = (row: number, col: number) => (row + col) % 2 === 1;

type Group = { label: string; cards: { label: string; title: string; text: string }[] };

export default async function FeaturesPage() {
  const t = await getTranslations("features");
  const groups = t.raw("groups") as Group[];
  const catalog = t.raw("catalog") as { title: string; text: string }[];
  const comparison = t.raw("comparison") as { label: string; without: string; with: string }[];
  return (
    <main>
      <section className={`${container} pt-16 md:pt-20`}>
        <div className="grid grid-cols-1 gap-8 md:grid-cols-[3fr_2fr] md:items-end">
          <div>
            <p className={eyebrow}>{t("eyebrow")}</p>
            <h1 className="mt-4 text-4xl font-semibold leading-[1.05] tracking-tight sm:text-5xl md:text-6xl">
              {t("title")}
            </h1>
          </div>
          <p className="text-base-content/80">
            {t("lead")}
          </p>
        </div>

        <div className="mt-12 space-y-10 pb-16 md:pb-20">
          {groups.map((g, gi) => (
            <section key={g.label}>
              <h2 className={`${eyebrow} border-t border-base-300 pt-4 text-base-content/70`}>{g.label}</h2>
              {/* subgrid: ilustrasi, judul, dan teks sejajar antar kartu dalam satu baris */}
              <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
                {g.cards.map((c, ci) => (
                  <Card key={c.title} shaded={isShaded(gi, ci)} image={groupImages[gi]![ci]!} label={c.label} title={c.title} text={c.text} />
                ))}
              </div>
            </section>
          ))}
        </div>
      </section>

      <section className="bg-white/60">
        <div className={`${container} py-16 md:py-20`}>
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <p className={eyebrow}>{t("catalogEyebrow")}</p>
              <h2 className="mt-4 max-w-xl text-3xl font-semibold tracking-tight">{t("catalogTitle")}</h2>
            </div>
            <p className="max-w-xs text-sm text-base-content/70">
              {t("catalogText")}
            </p>
          </div>
          <ol className="mt-10 grid grid-cols-1 gap-8 md:grid-cols-3">
            {catalog.map((c, i) => (
              <li key={c.title} className="border-t-2 border-base-content pt-4">
                <span className="text-xs text-base-content/60">0{i + 1}</span>
                <h3 className="mt-4 text-2xl font-semibold">{c.title}</h3>
                <p className="mt-3 text-sm text-base-content/70">{c.text}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className={`${container} py-16 md:py-20`}>
        <p className={eyebrow}>{t("compareEyebrow")}</p>
        <h2 className="mt-4 text-3xl font-semibold tracking-tight">{t("compareTitle")}</h2>
        <table className="mt-10 w-full border-t-2 border-base-content text-left text-sm">
          <thead className="border-b-2 border-base-content">
            <tr className="text-[10px] uppercase tracking-widest text-base-content/70">
              <th scope="col" className="py-3 pr-4 font-normal"><span className="sr-only">{t("aspect")}</span></th>
              <th scope="col" className="py-3 pr-4 font-normal">{t("without")}</th>
              <th scope="col" className="hidden sm:table-cell"><span className="sr-only">{t("becomes")}</span></th>
              <th scope="col" className="py-3 font-normal">{t("with")}</th>
            </tr>
          </thead>
          <tbody>
            {comparison.map((r) => (
              <tr key={r.label} className="border-b border-base-300 align-top sm:align-middle">
                <th scope="row" className="py-5 pr-4 text-[11px] font-normal tracking-wide text-base-content/70">{r.label}</th>
                <td className="py-5 pr-4 text-base-content/70">{r.without}</td>
                <td className="hidden px-4 text-base-content/40 sm:table-cell"><LuArrowRight aria-hidden className="size-3.5" /></td>
                <td className="py-5 font-semibold sm:text-base">{r.with}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <CtaBand title={t("ctaTitle")} text={t("ctaText")} />
    </main>
  );
}
