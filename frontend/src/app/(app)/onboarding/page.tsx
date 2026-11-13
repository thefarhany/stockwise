import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { LuLightbulb } from "react-icons/lu";
import { ProfileForm } from "@/features/onboarding";
import type { Outlet } from "@/features/onboarding/types";
import { coreGet } from "@/server/core-get";
import { OnboardingShell, TipCard } from "./_components/onboarding-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("onboarding"))("profileMeta")} · Stockwise` };
}

export default async function OnboardingPage() {
  const json = await coreGet<Outlet>("/outlet");
  // Hanya Owner yang boleh mengisi profil usaha.
  if (json && !json.success && json.errorCode === "FORBIDDEN") redirect("/dashboard");
  if (!json?.success) redirect("/login");
  const t = await getTranslations("onboarding");

  return (
    <OnboardingShell
      currency={json.data.currency}
      current={1}
      title={t("profileTitle")}
      lead={t("profileLead")}
      aside={
        <>
          <TipCard icon={<LuLightbulb className="size-4" aria-hidden />} title={t("tipClosingTitle")}>
            {t("tipClosingText")}
          </TipCard>
          <TipCard icon={<LuLightbulb className="size-4" aria-hidden />} title={t("tipEmptyTitle")}>
            {t("tipEmptyText")}
          </TipCard>
        </>
      }
    >
      <ProfileForm outlet={json.data} />
    </OnboardingShell>
  );
}
