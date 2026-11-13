import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { Guide } from "@/features/docs/components/guide";
import { getProfile } from "@/server/profile";
import { AppShell } from "../_components/app-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("docs"))("meta")} · Stockwise` };
}

/** Panduan versi aplikasi: di dalam sidebar, berbahasa akun; kasir hanya melihat bagian yang bisa ia lakukan. */
export default async function GuidePage() {
  const me = await getProfile();
  if (!me?.success) redirect("/login");
  const t = await getTranslations("docs");
  return (
    <AppShell profile={me.data} active="docs">
      <div className="mx-auto max-w-7xl">
        <header>
          <h1 className="text-3xl font-semibold tracking-tight">{t("appTitle")}</h1>
          <p className="mt-2 text-sm text-base-content/80">{t("lead")}</p>
        </header>
        <div className="mt-8">
          <Guide ownerSections={me.data.user.role === "OWNER"} tocTop="md:top-10" />
        </div>
      </div>
    </AppShell>
  );
}
