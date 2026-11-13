import Image from "next/image";
import Link from "next/link";
import { useTranslations } from "next-intl";
import {
  LuChartPie,
  LuClock,
  LuMousePointer2,
  LuTrendingUp,
} from "react-icons/lu";
import { CtaBand } from "./_components/cta-band";

const featureIcons = [LuMousePointer2, LuChartPie, LuClock, LuTrendingUp];

type Item = { title: string; text: string };

const container = "mx-auto max-w-6xl px-6";
const eyebrow = "text-xs uppercase tracking-widest";

export default function LandingPage() {
  const t = useTranslations("landing");
  const features = (t.raw("features") as Item[]).map((f, i) => ({
    ...f,
    Icon: featureIcons[i]!,
  }));
  const problems = t.raw("problems") as Item[];
  const steps = t.raw("steps") as Item[];
  const day = t.raw("day") as { time: string; you: string; app: string }[];
  return (
    <main>
      <section
        className={`${container} grid items-center gap-12 py-16 md:grid-cols-2 md:py-24`}
      >
        <div>
          <p className={eyebrow}>{t("eyebrow")}</p>
          <h1 className="mt-4 text-4xl font-semibold leading-[1.05] tracking-tight sm:text-5xl md:text-6xl">
            {t("title")}
          </h1>
          <p className="mt-6 max-w-md text-base-content/80">{t("lead")}</p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link href="/signup" className="btn btn-primary">
              {t("signUpFree")}
            </Link>
            <Link href="/how-it-works" className="btn border-base-300 bg-white">
              {t("seeHow")}
            </Link>
          </div>
        </div>
        {/* Ilustrasi hiasan (unDraw), judul di kiri sudah membawa pesannya. */}
        <Image
          src="/images/hero.svg"
          alt=""
          width={960}
          height={710}
          priority
          className="mx-auto h-auto w-full max-w-md"
        />
      </section>

      <section
        className={`${container} grid gap-8 pb-16 sm:grid-cols-2 lg:grid-cols-4`}
      >
        {features.map((f) => (
          <div key={f.title} className="border-t border-base-300 pt-6">
            <span className="grid size-8 place-items-center rounded-md bg-base-200">
              <f.Icon className="size-4" aria-hidden />
            </span>
            <h2 className="mt-4 font-semibold">{f.title}</h2>
            <p className="mt-2 text-sm text-base-content/70">{f.text}</p>
          </div>
        ))}
      </section>

      <section className="bg-neutral text-neutral-content">
        <div className={`${container} py-16`}>
          <p className={`${eyebrow} text-neutral-content/70`}>
            {t("quoteEyebrow")}
          </p>
          <blockquote className="mt-4 text-3xl tracking-tight md:text-4xl">
            {t("quote")}
          </blockquote>
          <p className="mt-4 text-sm text-neutral-content/70">
            {t("quoteText")}
          </p>
        </div>
      </section>

      <section
        className={`${container} grid gap-10 py-16 md:grid-cols-[1fr_2fr] md:py-20`}
      >
        <div>
          <p className={eyebrow}>{t("problemEyebrow")}</p>
          <h2 className="mt-4 text-3xl font-semibold tracking-tight">
            {t("problemTitle")}
          </h2>
        </div>
        <ol className="border-t border-base-300">
          {problems.map((p, i) => (
            <li
              key={p.title}
              className="grid grid-cols-1 gap-2 border-b border-base-300 py-5 sm:grid-cols-[2rem_9rem_1fr] sm:gap-4"
            >
              <span className="text-xs text-base-content/60">0{i + 1}</span>
              <h3 className="font-semibold">{p.title}</h3>
              <p className="text-sm text-base-content/70">{p.text}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="bg-white/60">
        <div className={`${container} py-16 md:py-20`}>
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <p className={eyebrow}>{t("howEyebrow")}</p>
              <h2 className="mt-4 text-3xl font-semibold tracking-tight">
                {t("howTitle")}
              </h2>
            </div>
            <p className="max-w-xs text-sm text-base-content/70">
              {t("howText")}
            </p>
          </div>
          <ol className="mt-10 grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-4">
            {steps.map((s, i) => (
              <li key={s.title} className="border-t border-base-300 pt-6">
                <span className="text-4xl text-secondary">0{i + 1}</span>
                <h3 className="mt-8 font-semibold">{s.title}</h3>
                <p className="mt-2 text-sm text-base-content/70">{s.text}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className={`${container} py-16 md:py-20`}>
        <p className={eyebrow}>{t("dayEyebrow")}</p>
        <h2 className="mt-4 text-3xl font-semibold tracking-tight">
          {t("dayTitle")}
        </h2>
        {/* subgrid: tiap baris kartu setinggi kartu terpanjang di baris itu, tanpa tinggi tetap yang bisa memotong teks */}
        <ol className="mt-10 grid grid-cols-1 gap-x-8 gap-y-8 sm:grid-cols-2 lg:grid-cols-4">
          {day.map((d) => (
            <li
              key={d.time}
              className="row-span-4 grid grid-rows-subgrid gap-y-0"
            >
              <div className="flex items-center gap-2" aria-hidden>
                <span className="size-2 rounded-full bg-base-content" />
                <span className="h-px flex-1 bg-base-300" />
              </div>
              <time className="mt-4 block text-2xl font-semibold">
                {d.time}
              </time>
              <div className="mt-4 rounded-box bg-neutral p-4 text-sm text-neutral-content">
                <p className={`${eyebrow} text-[10px] text-neutral-content/60`}>
                  {t("you")}
                </p>
                <p className="mt-2">{d.you}</p>
              </div>
              <div className="mt-3 rounded-box bg-base-200 p-4 text-sm">
                <p className={`${eyebrow} text-[10px] text-base-content/60`}>
                  Stockwise
                </p>
                <p className="mt-2">{d.app}</p>
              </div>
            </li>
          ))}
        </ol>
        <p className="mt-8 text-xs text-base-content/60">{t("autoClose")}</p>
      </section>

      <CtaBand title={t("ctaTitle")} text={t("ctaText")} />
    </main>
  );
}
