"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { LuChevronDown, LuLock, LuX } from "react-icons/lu";
import { Field } from "@/components/ui/field";
import { logout } from "@/features/auth";
import { FormError } from "@/features/auth/components/form-error";
import { ApiError, errorMessage } from "@/lib/api-client";
import { currencies, currencyCodes } from "@/lib/currency";
import { updateOutlet } from "../api/outlet";
import { addressInput, LocationField } from "./location-field";
import { EXIT_INTENT, ONBOARDING_FORM_ID } from "../constants";
import { profileSchema, type ProfileInput } from "../schemas";
import type { Outlet } from "../types";

const days = Array.from({ length: 31 }, (_, i) => i + 1);

/** `settings`: dipakai halaman Settings; simpan di tempat tanpa pindah langkah. */
export function ProfileForm({ outlet, mode = "onboarding" }: { outlet: Outlet; mode?: "onboarding" | "settings" }) {
  const router = useRouter();
  const t = useTranslations("profile");
  const tc = useTranslations("common");
  const tv = useTranslations("validation");
  const inSettings = mode === "settings";
  const [saved, setSaved] = useState(false);
  const { register, control, setValue, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<ProfileInput>({
    resolver: zodResolver(profileSchema(tv)),
    defaultValues: {
      name: outlet.name,
      currency: outlet.currency,
      // Backend mengirim "HH:mm:ss", input time memakai "HH:mm".
      openTime: outlet.openTime?.slice(0, 5) ?? "",
      closeTime: outlet.closeTime?.slice(0, 5) ?? "",
      latitude: outlet.latitude,
      longitude: outlet.longitude,
      address: addressInput(outlet.address),
      paydayDays: outlet.paydayDays,
      recommendationBufferPct: outlet.recommendationBufferPct,
      targetMarginPct: outlet.targetMarginPct,
    },
  });
  const [latitude, longitude, addressFull] = useWatch({ control, name: ["latitude", "longitude", "address.full"] });

  const onSubmit = handleSubmit(async (input, event) => {
    const submitter = (event?.nativeEvent as SubmitEvent | undefined)?.submitter as HTMLButtonElement | null;
    try {
      await updateOutlet(input);
      if (inSettings) {
        setSaved(true);
        // Nama dan jam buka juga tampil di sidebar.
        router.refresh();
      } else if (submitter?.value === EXIT_INTENT) {
        await logout().catch(() => {});
        router.replace("/login");
      } else {
        router.push("/onboarding/ingredients");
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) router.replace("/login");
      else setError("root", { message: e instanceof ApiError ? e.message : errorMessage("") });
    }
  });

  return (
    <form id={inSettings ? undefined : ONBOARDING_FORM_ID} onSubmit={onSubmit} onChange={() => setSaved(false)} noValidate className="space-y-6">
      <FormError message={errors.root?.message} />

      <Field id="name" label={t("businessName")} autoComplete="organization" placeholder={t("businessNamePlaceholder")} hint={t("businessNameHint")} error={errors.name?.message} {...register("name")} />

      <div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field id="openTime" label={t("openingTime")} type="time" error={errors.openTime?.message} {...register("openTime")} />
          <Field id="closeTime" label={t("closingTime")} type="time" error={errors.closeTime?.message} {...register("closeTime")} />
        </div>
        <p className="mt-2 text-xs text-base-content/60">
          {t("hoursHint")}
        </p>
      </div>

      <LocationField register={register} setValue={setValue} errors={errors} latitude={latitude} longitude={longitude} full={addressFull} />

      {outlet.currencyLocked ? (
        <p className="flex items-center gap-3 rounded-box bg-base-200 px-4 py-4 text-sm">
          <LuLock className="size-4 shrink-0" aria-hidden />
          {t("currencyLocked", { currency: `${t(`currencyNames.${outlet.currency}`)} (${currencies[outlet.currency].symbol})` })}
        </p>
      ) : (
        <div>
          <label htmlFor="currency" className="block text-sm font-semibold">{t("currency")}</label>
          <select id="currency" aria-describedby="currency-hint" className="select mt-1.5 w-full rounded-field border-base-300 bg-white/70" {...register("currency")}>
            {currencyCodes.map((c) => (
              <option key={c} value={c}>{t(`currencyNames.${c}`)} ({currencies[c].symbol})</option>
            ))}
          </select>
          <p id="currency-hint" className="mt-1.5 text-xs text-base-content/60">
            {t("currencyHint")}
          </p>
        </div>
      )}

      <p className="flex items-center gap-3 rounded-box bg-base-200 px-4 py-4 text-sm">
        <LuLock className="size-4 shrink-0" aria-hidden />
        {t("timezone", { timezone: outlet.timezone })}
      </p>

      <details open className="group rounded-box border border-base-300 bg-white/50 px-5 py-4">
        <summary className="flex cursor-pointer list-none items-start justify-between gap-4">
          <span>
            <span className="block text-sm font-semibold">{inSettings ? t("suggestionsTitle") : t("advancedSettings")}</span>
            <span className="mt-0.5 block text-xs text-base-content/60">
              {inSettings ? t("suggestionsLead") : t("defaultsLead")}
            </span>
          </span>
          <LuChevronDown className="mt-1 size-4 shrink-0 transition-transform group-open:rotate-180" aria-hidden />
        </summary>

        <div className="mt-5 space-y-5">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field id="recommendationBufferPct" label={t("buffer")} type="number" inputMode="decimal" step="any" suffix="%" hint={t("bufferHint")} error={errors.recommendationBufferPct?.message} {...register("recommendationBufferPct", { valueAsNumber: true })} />
            <Field id="targetMarginPct" label={t("targetMargin")} type="number" inputMode="decimal" step="any" suffix="%" hint={t("targetMarginHint")} error={errors.targetMarginPct?.message} {...register("targetMarginPct", { valueAsNumber: true })} />
          </div>

          <Controller
            control={control}
            name="paydayDays"
            render={({ field }) => (
              <div role="group" aria-labelledby="payday-label" aria-describedby="payday-hint">
                <p id="payday-label" className="text-sm font-semibold">{t("payday")}</p>
                <ul className="mt-2 flex flex-wrap gap-2">
                  {field.value.map((d) => (
                    <li key={d}>
                      <button
                        type="button"
                        onClick={() => field.onChange(field.value.filter((x) => x !== d))}
                        aria-label={t("removeDate", { day: d })}
                        className="btn btn-primary rounded-full"
                      >
                        {d} <LuX className="size-3.5" aria-hidden />
                      </button>
                    </li>
                  ))}
                  <li>
                    <select
                      aria-label={t("addDate")}
                      value=""
                      onChange={(e) => field.onChange([...field.value, Number(e.target.value)])}
                      className="select w-auto rounded-full border-base-300 bg-white"
                    >
                      <option value="" disabled>{t("addDateOption")}</option>
                      {days.filter((d) => !field.value.includes(d)).map((d) => (
                        <option key={d} value={d}>{d}</option>
                      ))}
                    </select>
                  </li>
                </ul>
                <p id="payday-hint" className="mt-2 text-xs text-base-content/60">
                  {t("paydayHint")}
                </p>
              </div>
            )}
          />
        </div>
      </details>

      <div className="flex items-center justify-end gap-4">
        {saved && <p className="text-sm text-success" role="status">{t("saved")}</p>}
        <button type="submit" disabled={isSubmitting} className="btn btn-primary">
          {isSubmitting ? tc("saving") : inSettings ? t("saveProfile") : t("continueToIngredients")}
        </button>
      </div>
    </form>
  );
}
