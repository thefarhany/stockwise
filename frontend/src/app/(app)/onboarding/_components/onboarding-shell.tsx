import Link from "next/link";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import { LuCheck } from "react-icons/lu";
import { CurrencyProvider } from "@/components/currency";
import { LanguageSelect } from "@/components/language-select";
import { EXIT_INTENT, ONBOARDING_FORM_ID } from "@/features/onboarding/constants";
import type { Currency } from "@/lib/currency";

const stepKeys = ["account", "profile", "ingredients", "products"] as const;

/** Kerangka langkah onboarding: header, daftar langkah di kiri, isi di tengah, panel bantuan di kanan. */
export function OnboardingShell({
  current,
  title,
  lead,
  aside,
  children,
  eyebrow,
  headerAction,
  currency,
}: {
  /** Mata uang usaha, untuk isian dan tampilan uang di dalamnya. */
  currency: Currency;
  /** Indeks langkah yang aktif, mulai 0 (Account); sama dengan jumlah langkah bila semua selesai. */
  current: number;
  /** Pengganti "Step N of 5" di atas judul. */
  eyebrow?: ReactNode;
  /** Pengganti tombol "Save and exit" untuk halaman tanpa form. */
  headerAction?: ReactNode;
  title: string;
  lead: string;
  aside: ReactNode;
  children: ReactNode;
}) {
  const t = useTranslations("onboarding");
  const tc = useTranslations("common");
  const steps = stepKeys.map((k) => t(`steps.${k}`));
  return (
    <CurrencyProvider currency={currency}>
      <header className="border-b border-base-300">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-2 px-4 sm:px-6">
          <Link href="/" className="flex items-center gap-2 text-lg font-semibold">
            <span className="grid size-7 place-items-center rounded-md bg-base-content text-xs text-base-100">S</span>
            <span className="max-sm:sr-only">Stockwise</span>
          </Link>
          <div className="flex items-center gap-2 sm:gap-4">
            <LanguageSelect scope="account" />
            {headerAction ?? (
              <button type="submit" form={ONBOARDING_FORM_ID} value={EXIT_INTENT} className="btn btn-sm border-base-300 bg-white">
                {t("saveAndExit")}
              </button>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto grid w-full max-w-7xl flex-1 grid-cols-1 gap-10 px-4 py-12 sm:px-6 lg:grid-cols-[13rem_1fr_18rem] lg:gap-12">
        <nav aria-label={t("setupSteps")}>
          <p className="text-xs uppercase tracking-widest text-base-content/70">{t("businessSetup")}</p>
          <ol className="mt-5 flex gap-4 overflow-x-auto lg:block lg:space-y-6">
            {steps.map((label, i) => (
              <li key={label} aria-current={i === current ? "step" : undefined} className="flex shrink-0 items-center gap-3 text-sm">
                <span
                  className={`grid size-7 place-items-center rounded-full text-xs ${
                    i < current
                      ? "bg-base-content text-base-100"
                      : i === current
                        ? "border-2 border-base-content font-semibold"
                        : "bg-base-200 text-base-content/70"
                  }`}
                >
                  {i < current ? <LuCheck className="size-3.5" aria-label={t("done")} /> : i + 1}
                </span>
                <span className={i === current ? "font-semibold" : i > current ? "text-base-content/70" : ""}>{label}</span>
              </li>
            ))}
          </ol>
          {current < steps.length && (
            <p className="mt-6 hidden text-xs text-base-content/70 lg:block">
              {t("under10")}
            </p>
          )}
        </nav>

        <div className="max-w-2xl">
          <div className="text-xs uppercase tracking-widest text-base-content/70">
            {eyebrow ?? t("stepOf", { current: current + 1, total: steps.length })}
          </div>
          <h1 className="mt-3 text-4xl font-semibold tracking-tight md:text-5xl">{title}</h1>
          <p className="mt-3 text-base-content/80">{lead}</p>
          <div className="mt-8">{children}</div>
        </div>

        <aside className="space-y-4">{aside}</aside>
      </main>

      <footer className="border-t border-base-300">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-5 text-xs text-base-content/70">
          <p>© 2026 Stockwise</p>
          <div className="flex gap-6">
            <a href="mailto:info@stockwise.com" className="hover:text-base-content">{t("help")}</a>
            <Link href="/privacy" className="hover:text-base-content">{tc("privacyPolicy")}</Link>
          </div>
        </div>
      </footer>
    </CurrencyProvider>
  );
}

/** Kartu tips abu-abu di panel kanan. */
export function TipCard({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
  return (
    <section className="rounded-box bg-base-200 p-6">
      <span className="grid size-9 place-items-center rounded-field bg-white">{icon}</span>
      <h2 className="mt-4 text-lg font-semibold">{title}</h2>
      <p className="mt-2 text-sm text-base-content/80">{children}</p>
    </section>
  );
}
