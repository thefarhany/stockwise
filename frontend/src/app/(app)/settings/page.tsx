import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { LanguageSelect } from "@/components/language-select";
import type { User } from "@/features/auth/types";
import { ProfileForm } from "@/features/onboarding";
import type { Outlet } from "@/features/onboarding/types";
import { DeleteOutletSection } from "@/features/settings/delete-outlet-section";
import { StaffSection } from "@/features/settings/staff-section";
import { coreGet } from "@/server/core-get";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("nav"))("settings")} · Stockwise` };
}

export default async function SettingsPage() {
  const me = await getProfile();
  if (!me?.success) redirect("/login");
  const owner = me.data.user.role === "OWNER";
  const t = await getTranslations("settings");

  const language = (
    <section id="language" aria-labelledby="language-heading" className="scroll-mt-8 rounded-box border border-base-300 bg-white/60">
      <div className="flex flex-wrap items-center justify-between gap-4 px-7 py-6">
        <div>
          <h2 id="language-heading" className="text-xl font-semibold">{t("languageTitle")}</h2>
          <p className="mt-1 text-sm text-base-content/70">{t("languageLead")}</p>
        </div>
        <LanguageSelect scope="account" />
      </div>
    </section>
  );

  // Kasir hanya melihat pilihan bahasa; /outlet dan /users khusus Owner.
  if (!owner)
    return (
      <AppShell profile={me.data} active="settings">
        <div className="mx-auto max-w-7xl">
          <h1 className="text-3xl font-semibold tracking-tight">{t("title")}</h1>
          <p className="mt-2 text-sm text-base-content/80">{t("leadStaff")}</p>
          <div className="mt-6">{language}</div>
        </div>
      </AppShell>
    );

  const [outlet, users] = await Promise.all([coreGet<Outlet>("/outlet"), coreGet<User[]>("/users")]);
  if (!outlet?.success || !users?.success) redirect("/login");
  const o = outlet.data;

  return (
    <AppShell profile={me.data} active="settings">
      <div className="mx-auto max-w-7xl">
        <h1 className="text-3xl font-semibold tracking-tight">{t("title")}</h1>
        <p className="mt-2 text-sm text-base-content/80">{t("lead")}</p>

        <div className="mt-6 space-y-6">
          {language}

          <section id="profile" aria-labelledby="profile-heading" className="scroll-mt-8 rounded-box border border-base-300 bg-white/60">
            <div className="border-b border-base-300 px-7 py-6">
              <h2 id="profile-heading" className="text-xl font-semibold">{t("profileTitle")}</h2>
              <p className="mt-1 text-sm text-base-content/70">{t("profileLead")}</p>
            </div>
            <div className="px-7 py-6">
              <ProfileForm outlet={o} mode="settings" />
            </div>
          </section>

          <StaffSection users={users.data} meId={me.data.user.id} />

          <DeleteOutletSection
            name={o.name}
            counts={{ users: users.data.length, stockItems: o.onboarding.stockItems, products: o.onboarding.products }}
          />
        </div>
      </div>
    </AppShell>
  );
}
