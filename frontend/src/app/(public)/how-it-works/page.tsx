import type { Metadata } from "next";
import Image from "next/image";
import { getTranslations } from "next-intl/server";
import type { IconType } from "react-icons";
import { LuCalendar, LuCloud } from "react-icons/lu";
import { CtaBand } from "../_components/cta-band";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("how");
  return { title: `${t("meta")} · Stockwise`, description: t("metaDescription") };
}

const stepIds = ["open", "sell", "close", "summary"];

/** Ilustrasi per langkah (file di public/images), ukuran asli file untuk rasio. */
const stepImages: Record<string, { src: string; width: number; height: number }> = {
  open: { src: "/images/open-stall.svg", width: 960, height: 539 },
  sell: { src: "/images/sell.svg", width: 884, height: 453 },
  close: { src: "/images/close-stall.svg", width: 790, height: 800 },
  summary: { src: "/images/summary-suggestion.svg", width: 800, height: 585 },
};

// Ilustrasi saja: tinggi batang dalam persen; Sabtu (indeks 5) ditandai dengan rentang saran.
const SATURDAY = 5;
const stageVisuals: { dot: string; chipIcons: (IconType | undefined)[]; bars: number[]; saturday: { value: number; upper: number } }[] = [
  { dot: "border border-base-content", chipIcons: [], bars: [40, 40, 40, 40, 40, 0, 40], saturday: { value: 30, upper: 80 } },
  { dot: "bg-base-content/50", chipIcons: [], bars: [30, 38, 35, 45, 50, 0, 75], saturday: { value: 65, upper: 92 } },
  { dot: "bg-base-content", chipIcons: [undefined, LuCalendar, LuCloud], bars: [30, 38, 35, 45, 50, 0, 72], saturday: { value: 68, upper: 78 } },
];

type Step = { title: string; text: string; points: string[] };
type Stage = { label: string; title: string; sessions: string; chips: string[]; text: string };

const container = "mx-auto max-w-6xl px-6";
const eyebrow = "text-xs uppercase tracking-widest";

function DashList({ items }: { items: string[] }) {
  return (
    <ul className="mt-6 border-t border-base-300">
      {items.map((item) => (
        <li key={item} className="flex gap-4 border-b border-base-300 py-3 text-sm">
          <span aria-hidden className="text-base-content/40">—</span>
          {item}
        </li>
      ))}
    </ul>
  );
}

export default async function HowItWorksPage() {
  const t = await getTranslations("how");
  const steps = (t.raw("steps") as Step[]).map((st, i) => ({ ...st, id: stepIds[i]! }));
  const stages = (t.raw("stages") as Stage[]).map((st, i) => ({ ...st, ...stageVisuals[i]! }));
  const days = t.raw("days") as string[];
  const setup = t.raw("setup") as { title: string; text: string }[];
  const roles = t.raw("roles") as { title: string; subtitle: string; points: string[] }[];
  return (
    <main>
      <section className={`${container} py-16 md:py-20`}>
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
        <ol className="mt-12 grid grid-cols-2 gap-x-6 gap-y-4 text-sm md:grid-cols-4">
          {steps.map((s, i) => (
            <li key={s.id} className="border-t border-base-300 pt-3">
              <a href={`#${s.id}`} className="hover:underline">
                <span className="mr-2 text-base-content/60">0{i + 1}</span>
                {s.title}
              </a>
            </li>
          ))}
        </ol>
      </section>

      {steps.map((s, i) => (
        <section key={s.id} id={s.id} className={i % 2 ? "bg-white/60" : "bg-base-100"}>
          <div className={`${container} grid items-center gap-10 py-16 md:grid-cols-2 md:py-20`}>
            <div className={i % 2 ? "md:order-2" : undefined}>
              <span className="text-4xl text-secondary">0{i + 1}</span>
              <h2 className="mt-3 text-3xl font-semibold tracking-tight">{s.title}</h2>
              <p className="mt-4 text-base-content/80">{s.text}</p>
              <DashList items={s.points} />
            </div>
            <Image {...stepImages[s.id]!} alt="" className="mx-auto h-auto w-full max-w-md" />
          </div>
        </section>
      ))}

      <section className={`${container} py-16 md:py-20`}>
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <p className={eyebrow}>{t("suggestionEyebrow")}</p>
            <h2 className="mt-4 text-3xl font-semibold tracking-tight">{t("suggestionTitle")}</h2>
          </div>
          <p className="max-w-xs text-sm text-base-content/70">
            {t("suggestionText")}
          </p>
        </div>
        <ol className="mt-10 grid grid-cols-1 gap-8 md:grid-cols-3">
          {stages.map((st) => (
            <li key={st.label}>
              <div className="flex items-center gap-2" aria-hidden>
                <span className={`size-2.5 rounded-full ${st.dot}`} />
                <span className="h-px flex-1 bg-base-300" />
              </div>
              <p className={`${eyebrow} mt-4 text-[10px] text-base-content/60`}>{st.label}</p>
              <h3 className="mt-1 text-xl font-semibold">{st.title}</h3>
              <p className="text-xs text-base-content/60">{st.sessions}</p>

              <figure className="mt-4 rounded-box border border-base-300 bg-white p-4" aria-hidden>
                <div className="flex flex-wrap gap-1.5">
                  {st.chips.map((label, ci) => {
                    const Icon = st.chipIcons[ci];
                    return (
                      <span key={label} className="flex items-center gap-1 rounded-full bg-base-200 px-2 py-0.5 text-[10px]">
                        {Icon && <Icon className="size-3" />}
                        {label}
                      </span>
                    );
                  })}
                </div>
                <div className="mt-4 flex h-24 items-end gap-2">
                  {days.map((d, di) => (
                    <div key={d} className="flex h-full flex-1 flex-col items-center justify-end gap-1">
                      {di === SATURDAY ? (
                        <div className="flex w-full flex-col justify-end rounded-sm bg-base-content/30" style={{ height: `${st.saturday.upper}%` }}>
                          <div className="rounded-sm bg-base-content" style={{ height: `${(st.saturday.value / st.saturday.upper) * 100}%` }} />
                        </div>
                      ) : (
                        <div className="w-full rounded-sm bg-base-300" style={{ height: `${st.bars[di]}%` }} />
                      )}
                    </div>
                  ))}
                </div>
                <div className="mt-1 flex gap-2 text-center text-[9px] text-base-content/60">
                  {days.map((d, di) => (
                    <span key={d} className={`flex-1 ${di === SATURDAY ? "font-semibold text-base-content" : ""}`}>{d}</span>
                  ))}
                </div>
                <figcaption className="mt-3 flex items-center justify-center gap-1.5 text-[10px] text-base-content/60">
                  <span className="h-2 w-3 rounded-sm bg-base-content/30" />
                  {t("range")}
                </figcaption>
              </figure>
              <p className="mt-4 text-xs text-base-content/70">{st.text}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="bg-neutral text-neutral-content">
        <div className={`${container} grid gap-10 py-16 md:grid-cols-[2fr_3fr] md:py-20`}>
          <div>
            <p className={`${eyebrow} text-neutral-content/70`}>{t("firstEyebrow")}</p>
            <h2 className="mt-4 text-3xl font-semibold tracking-tight">{t("firstTitle")}</h2>
            <p className="mt-4 text-sm text-neutral-content/70">
              {t("firstText")}
            </p>
          </div>
          <div>
            <ol className="border-t border-neutral-content/15">
              {setup.map((s, i) => (
                <li key={s.title} className="flex gap-6 border-b border-neutral-content/15 py-5">
                  <span className="w-10 shrink-0 text-3xl text-neutral-content/80">0{i + 1}</span>
                  <div>
                    <h3 className="font-semibold">{s.title}</h3>
                    <p className="mt-1 text-sm text-neutral-content/70">{s.text}</p>
                  </div>
                </li>
              ))}
            </ol>
            <p className="mt-6 font-semibold">{t("thenOpen")}</p>
          </div>
        </div>
      </section>

      <section className={`${container} py-16 md:py-20`}>
        <p className={eyebrow}>{t("rolesEyebrow")}</p>
        <h2 className="mt-4 text-3xl font-semibold tracking-tight">{t("rolesTitle")}</h2>
        <div className="mt-10 grid grid-cols-1 gap-10 md:grid-cols-2">
          {roles.map((r) => (
            <div key={r.title} className="border-t-2 border-base-content pt-6">
              <h3 className="text-xl font-semibold">{r.title}</h3>
              <p className="text-xs text-base-content/60">{r.subtitle}</p>
              <DashList items={r.points} />
            </div>
          ))}
        </div>
      </section>

      <CtaBand title={t("ctaTitle")} text={t("ctaText")} />
    </main>
  );
}
