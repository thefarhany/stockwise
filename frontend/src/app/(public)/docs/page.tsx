import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { Guide } from "@/features/docs/components/guide";
import { CtaBand } from "../_components/cta-band";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("docs");
  return { title: `${t("meta")} · Stockwise`, description: t("metaDescription") };
}

const container = "mx-auto max-w-6xl px-6";

export default async function DocsPage() {
  const t = await getTranslations("docs");
  return (
    <main>
      <section className={`${container} grid grid-cols-1 gap-8 py-16 md:grid-cols-2 md:items-end md:py-20`}>
        <div>
          <p className="text-xs uppercase tracking-widest">{t("eyebrow")}</p>
          <h1 className="mt-4 text-4xl font-semibold leading-[1.05] tracking-tight sm:text-5xl md:text-6xl">{t("title")}</h1>
        </div>
        <p className="text-base-content/80">{t("lead")}</p>
      </section>

      <div className={`${container} pb-16 md:pb-20`}>
        <Guide tocTop="md:top-24" />
      </div>

      <CtaBand title={t("ctaTitle")} text={t("ctaText")} />
    </main>
  );
}
