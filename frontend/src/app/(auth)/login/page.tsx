import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { LoginForm } from "@/features/auth";
import { AuthShell } from "../_components/auth-shell";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("auth");
  return { title: `${t("titleLogin")} · Stockwise` };
}

export default async function LoginPage() {
  const t = await getTranslations("auth");
  return (
    <AuthShell
      aside={
        <>
          <p className="text-xs uppercase tracking-widest text-neutral-content/70">{t("loginEyebrow")}</p>
          <h2 className="mt-6 text-5xl font-medium leading-[1.1] tracking-tight">{t("loginAsideTitle")}</h2>
          <p className="mt-8 max-w-md text-lg text-neutral-content/70">{t("loginAsideText")}</p>
        </>
      }
    >
      <h1 className="text-4xl font-semibold tracking-tight">{t("loginTitle")}</h1>
      <p className="mt-3 text-base-content/80">{t("loginLead")}</p>
      <div className="mt-8"><LoginForm /></div>
    </AuthShell>
  );
}
