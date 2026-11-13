import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getLocale, getTranslations } from "next-intl/server";
import { LuCheck } from "react-icons/lu";
import { LogoutButton } from "@/components/logout-button";
import type { Profile } from "@/features/auth/types";
import { nextOnboardingStep } from "@/features/onboarding/next-step";
import type { Outlet } from "@/features/onboarding/types";
import { hhmm } from "@/lib/format";
import { coreGet } from "@/server/core-get";
import { OnboardingShell } from "../_components/onboarding-shell";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("onboarding"))("doneMeta")} · Stockwise` };
}

export default async function DonePage() {
  const [outlet, me] = await Promise.all([coreGet<Outlet>("/outlet"), coreGet<Profile>("/me")]);
  if (outlet && !outlet.success && outlet.errorCode === "FORBIDDEN") redirect("/dashboard");
  if (!outlet?.success || !me?.success) redirect("/login");
  const unfinished = nextOnboardingStep(outlet.data.onboarding);
  if (unfinished) redirect(unfinished);

  const t = await getTranslations("onboarding");
  const listFormat = new Intl.ListFormat(await getLocale(), { type: "conjunction" });
  const requirements = [t("reqProfile"), t("reqIngredient"), t("reqProduct"), t("reqRecipe")];
  const nextUp = [
    { title: t("nextOpenTitle"), text: t("nextOpenText") },
    { title: t("nextSellTitle"), text: t("nextSellText") },
    { title: t("nextCloseTitle"), text: t("nextCloseText") },
  ];
  const o = outlet.data;
  const { user } = me.data;
  const details = [
    { label: t("detailOwner"), value: `${user.name} · ${user.email}` },
    { label: t("detailHours"), value: `${hhmm(o.openTime)} – ${hhmm(o.closeTime)}` },
    { label: t("detailLocation"), value: o.address?.full ?? (o.latitude !== null && o.longitude !== null ? `${o.latitude}, ${o.longitude}` : t("notSetYet")) },
    { label: t("detailTimezone"), value: o.timezone },
    { label: t("detailPayday"), value: o.paydayDays.length > 0 ? listFormat.format(o.paydayDays.map(String)) : t("none") },
    { label: t("detailSuggestions"), value: t("suggestionsValue", { buffer: o.recommendationBufferPct, margin: o.targetMarginPct }) },
  ];

  return (
    <OnboardingShell
      currency={o.currency}
      current={4}
      headerAction={<LogoutButton className="btn btn-sm border-base-300 bg-white" />}
      eyebrow={
        <>
          <span className="mb-8 grid size-16 place-items-center rounded-full bg-base-content text-base-100">
            <LuCheck className="size-7" aria-hidden />
          </span>
          {t("done")}
        </>
      }
      title={t("doneTitle")}
      lead={t("doneLead", { name: o.name })}
      aside={
        <>
          <section aria-labelledby="req-heading" className="rounded-box border border-base-300 bg-white/50 p-6">
            <h2 id="req-heading" className="border-b border-base-300 pb-4 text-sm font-semibold">{t("requirements")}</h2>
            <ul className="divide-y divide-base-300">
              {requirements.map((r) => (
                <li key={r} className="flex items-center gap-3 py-5 text-sm">
                  <span className="grid size-6 shrink-0 place-items-center rounded-full bg-base-content text-base-100">
                    <LuCheck className="size-3.5" aria-label={t("met")} />
                  </span>
                  {r}
                </li>
              ))}
            </ul>
          </section>
          <section aria-labelledby="next-heading" className="rounded-box bg-base-200 p-6">
            <h2 id="next-heading" className="text-xl font-semibold">{t("whatNext")}</h2>
            <dl className="mt-4 space-y-4">
              {nextUp.map((n) => (
                <div key={n.title}>
                  <dt className="text-sm font-semibold">{n.title}</dt>
                  <dd className="mt-0.5 text-xs text-base-content/70">{n.text}</dd>
                </div>
              ))}
            </dl>
          </section>
        </>
      }
    >
      <section aria-labelledby="outlet-heading" className="overflow-hidden rounded-box border border-base-300 bg-white/50">
        <div className="flex items-center justify-between gap-4 border-b border-base-300 px-6 py-5">
          <h2 id="outlet-heading" className="truncate text-xl font-semibold">{o.name}</h2>
          <span className="flex shrink-0 items-center gap-2 rounded-full bg-base-200 px-3 py-1 text-sm">
            <span aria-hidden className="size-2 rounded-full bg-success" /> {t("readyToOpen")}
          </span>
        </div>
        <dl className="divide-y divide-base-300 text-sm">
          {details.map((d) => (
            <div key={d.label} className="grid grid-cols-1 gap-1 px-6 py-4 sm:grid-cols-[12rem_1fr]">
              <dt className="text-base-content/70">{d.label}</dt>
              <dd>{d.value}</dd>
            </div>
          ))}
        </dl>
      </section>

      <div className="mt-8 flex flex-wrap gap-3">
        <Link href="/open" className="btn btn-primary">{t("openStall")}</Link>
        <Link href="/onboarding/products" className="btn border-base-300 bg-white">{t("addAnotherProduct")}</Link>
      </div>
      <p className="mt-10 text-sm text-base-content/70">
        {t("changeLater")}
      </p>
    </OnboardingShell>
  );
}
