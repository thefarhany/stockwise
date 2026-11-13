import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { LuCheck } from "react-icons/lu";
import { SignupForm } from "@/features/auth";
import { AuthShell } from "../_components/auth-shell";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("auth");
  return { title: `${t("titleSignup")} · Stockwise` };
}

export default async function SignupPage() {
  const t = await getTranslations("auth");
  const steps = [
    { title: t("signupStep1Title"), text: t("signupStep1Text") },
    { title: t("signupStep2Title"), text: t("signupStep2Text") },
    { title: t("signupStep3Title"), text: t("signupStep3Text") },
  ];
  return (
    <AuthShell
      aside={
        <>
          <p className="text-xs uppercase tracking-widest text-neutral-content/70">{t("signupEyebrow")}</p>
          <h2 className="mt-6 text-5xl font-medium leading-[1.1] tracking-tight">{t("signupAsideTitle")}</h2>
          <ol className="mt-10 border-t border-neutral-content/30">
            {steps.map((s, i) => (
              <li key={s.title} className="flex gap-5 border-b border-neutral-content/30 py-6 last:border-b-0">
                <span
                  aria-hidden
                  className={`grid size-9 shrink-0 place-items-center rounded-full text-sm ${i === 0 ? "bg-neutral-content text-neutral" : "border border-neutral-content/60"}`}
                >
                  {i === 0 ? <LuCheck className="size-4" /> : i + 1}
                </span>
                <div>
                  <h3 className="text-xl font-semibold">{s.title}</h3>
                  <p className="mt-1 text-sm text-neutral-content/70">{s.text}</p>
                </div>
              </li>
            ))}
          </ol>
          <p className="mt-10 text-sm text-neutral-content/70">{t("signupAsideNote")}</p>
        </>
      }
    >
      <h1 className="text-3xl font-semibold tracking-tight lg:text-4xl">{t("signupTitle")}</h1>
      <p className="mt-2 text-base-content/80">{t("signupLead")}</p>
      <div className="mt-5"><SignupForm /></div>
    </AuthShell>
  );
}
