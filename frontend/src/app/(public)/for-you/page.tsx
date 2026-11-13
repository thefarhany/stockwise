import type { Metadata } from "next";
import Image from "next/image";
import { getTranslations } from "next-intl/server";
import type { IconType } from "react-icons";
import { LuArrowRight, LuLayers, LuMinus, LuMousePointer2, LuPlus, LuSlidersHorizontal } from "react-icons/lu";
import { CtaBand } from "../_components/cta-band";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("forYou");
  return { title: `${t("meta")} · Stockwise`, description: t("metaDescription") };
}

const container = "mx-auto max-w-6xl px-6";
const eyebrow = "text-xs uppercase tracking-widest";
/** Ilustrasi per jenis usaha (file di public/images), urutannya sama dengan forYou.businesses di messages. */
const businessImages = [
  { src: "/images/snacks-deserts.svg", width: 750, height: 500 },
  { src: "/images/bakery-pastry.svg", width: 500, height: 500 },
  { src: "/images/drinks.svg", width: 500, height: 500 },
];

const tag = "w-fit rounded-full bg-base-200 px-2.5 py-0.5 text-[10px]";

/** Tampilan tiap contoh barang, urutannya sama dengan forYou.examples di messages. */
const exampleStyles: { Icon: IconType; dark?: boolean; open?: boolean }[] = [
  { Icon: LuLayers, dark: true },
  { Icon: LuMousePointer2 },
  { Icon: LuMousePointer2 },
  { Icon: LuSlidersHorizontal, open: true },
];

type Example = { name: string; note: string; ingredients: string[]; product: string; mode: string };

function Chip({ label, dark, invert, dashed }: { label: string; dark?: boolean; invert?: boolean; dashed?: boolean }) {
  const style = dashed
    ? "border border-dashed border-base-content/40"
    : invert
      ? "bg-white text-base-content"
      : dark
        ? "bg-base-content text-base-100"
        : "border border-current";
  return <span className={`rounded-full px-2.5 py-0.5 text-[10px] ${style}`}>{label}</span>;
}

export default async function ForYouPage() {
  const t = await getTranslations("forYou");
  const businesses = t.raw("businesses") as { title: string; mode: string; text: string }[];
  const fit = t.raw("fit") as string[];
  const notFit = t.raw("notFit") as string[];
  const examples = (t.raw("examples") as Example[]).map((e, i) => ({ ...e, ...exampleStyles[i]! }));
  const faqs = t.raw("faqs") as { q: string; a: string }[];
  return (
    <main>
      <section className={`${container} py-16 md:py-20`}>
        <div className="grid grid-cols-1 gap-8 md:grid-cols-[3fr_2fr] md:items-end">
          <div>
            <p className={eyebrow}>{t("eyebrow")}</p>
            <h1 className="mt-4 text-4xl font-semibold leading-[1.05] tracking-tight sm:text-5xl md:text-6xl">{t("title")}</h1>
          </div>
          <p className="text-base-content/80">
            {t("lead")}
          </p>
        </div>
      </section>

      <section className="bg-white/60">
        <div className={`${container} py-16 md:py-20`}>
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <p className={eyebrow}>{t("bizEyebrow")}</p>
              <h2 className="mt-4 text-3xl font-semibold tracking-tight">{t("bizTitle")}</h2>
            </div>
            <p className="max-w-xs text-sm text-base-content/70">
              {t("bizText")}
            </p>
          </div>
          <ul className="mt-10 grid grid-cols-1 gap-6 md:grid-cols-3">
            {businesses.map((b, i) => (
              // Section ini berlatar putih, jadi kartunya memakai warna latar halaman supaya tetap terlihat sebagai kartu.
              <li key={b.title} className="rounded-box border border-base-300 bg-base-100 p-5">
                <Image {...businessImages[i]!} alt="" className="mx-auto h-40 w-full max-w-60 object-contain" />
                <h3 className="mt-4 text-xl font-semibold">{b.title}</h3>
                <p className={`${tag} mt-2`}>{b.mode}</p>
                <p className="mt-3 text-sm text-base-content/70">{b.text}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className={`${container} py-16 md:py-20`}>
        <p className={eyebrow}>{t("fitEyebrow")}</p>
        <h2 className="mt-4 text-3xl font-semibold tracking-tight">{t("fitTitle")}</h2>
        <div className="mt-10 grid grid-cols-1 gap-10 md:grid-cols-2">
          {[
            { title: t("fitsIf"), items: fit, mark: "+", heading: "" },
            { title: t("notFitsIf"), items: notFit, mark: "–", heading: "text-secondary" },
          ].map((col) => (
            <div key={col.title} className="border-t-2 border-base-content pt-6">
              <h3 className={`text-xl font-semibold ${col.heading}`}>{col.title}</h3>
              <ul className="mt-4 border-t border-base-300">
                {col.items.map((item) => (
                  <li key={item} className="flex gap-4 border-b border-base-300 py-3 text-sm">
                    <span aria-hidden className="text-base-content/50">{col.mark}</span>
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-base-200">
        <div className={`${container} py-16 md:py-20`}>
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <p className={eyebrow}>{t("examplesEyebrow")}</p>
              <h2 className="mt-4 text-3xl font-semibold tracking-tight">{t("examplesTitle")}</h2>
            </div>
            <p className="max-w-xs text-sm text-base-content/70">
              {t("examplesText")}
            </p>
          </div>

          <div className="mt-10 hidden grid-cols-[2fr_2.5fr_2rem_1.5fr_2fr] gap-4 px-4 text-[10px] uppercase tracking-widest text-base-content/60 md:grid">
            <span>{t("colBusiness")}</span><span>{t("colIngredients")}</span><span /><span>{t("colProduct")}</span><span>{t("colMode")}</span>
          </div>
          <ul className="mt-3 space-y-3">
            {examples.map((e) => (
              <li
                key={e.name}
                className={`grid gap-3 rounded-box p-4 md:grid-cols-[2fr_2.5fr_2rem_1.5fr_2fr] md:items-center md:gap-4 ${e.dark ? "bg-neutral text-neutral-content" : "bg-white/80"}`}
              >
                <div>
                  <p className="font-semibold">{e.name}</p>
                  <p className={`text-xs ${e.dark ? "text-neutral-content/60" : "text-base-content/60"}`}>{e.note}</p>
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {e.ingredients.map((i) => <Chip key={i} label={i} dashed={e.open} />)}
                </div>
                <LuArrowRight aria-label={t("becomes")} className="hidden size-3.5 md:block" />
                <div><Chip label={e.product} dark={!e.dark && !e.open} invert={e.dark} dashed={e.open} /></div>
                <p className="flex items-center gap-2 text-xs"><e.Icon aria-hidden className="size-3.5" />{e.mode}</p>
              </li>
            ))}
          </ul>
          <p className="mt-6 text-xs text-base-content/60">{t("modeNote")}</p>
        </div>
      </section>

      <section className={`${container} grid gap-10 py-16 md:grid-cols-[1fr_2fr] md:py-20`}>
        <div>
          <p className={eyebrow}>{t("faqEyebrow")}</p>
          <h2 className="mt-4 text-3xl font-semibold tracking-tight">{t("faqTitle")}</h2>
          <p className="mt-4 text-xs text-base-content/60">{t("faqNote")}</p>
        </div>
        <div className="border-t-2 border-base-content">
          {faqs.map((f, i) => (
            <details key={f.q} open={i === 0} className="group border-b border-base-300">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-5 font-semibold [&::-webkit-details-marker]:hidden">
                {f.q}
                <LuPlus aria-hidden className="size-4 shrink-0 group-open:hidden" />
                <LuMinus aria-hidden className="hidden size-4 shrink-0 group-open:block" />
              </summary>
              <p className="pb-5 text-sm text-base-content/70">{f.a}</p>
            </details>
          ))}
        </div>
      </section>

      <CtaBand title={t("ctaTitle")} text={t("ctaText")} />
    </main>
  );
}
