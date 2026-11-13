"use client";

import { useLocale, useTranslations } from "next-intl";
import type { ComponentProps } from "react";
import { Controller, useWatch, type UseFormReturn } from "react-hook-form";
import { LuMinus, LuPlus } from "react-icons/lu";
import { Field } from "@/components/ui/field";
import { MoneyField } from "@/components/ui/money-field";
import type { StockItemInput } from "@/features/catalog/schemas";
import { stockUnitKeys, stockUnits, unitLabel } from "@/features/catalog/units";

export const stockItemDefaults: StockItemInput = {
  name: "",
  kind: "RAW",
  unit: "GRAM",
  unitCost: 0,
  shelfLifeDays: 1,
  carryOverAllowed: true,
  isPrimary: false,
};

/** Pintasan umur simpan (hari); bulan dihitung 30 hari. */
const shelfLifePresets = [
  [7, "week1"],
  [14, "week2"],
  [30, "month1"],
  [90, "month3"],
  [180, "month6"],
  [365, "year1"],
] as const;

const stockKinds = ["RAW", "FINISHED"] as const;

/**
 * Isian bahan, dipakai onboarding (satu kolom) dan modal Ingredients (dua kolom).
 * `unitLocked`: backend menolak ganti satuan untuk bahan yang sudah dipakai di sesi.
 */
export function StockItemFields({
  form,
  columns = false,
  unitLocked = false,
}: {
  form: UseFormReturn<StockItemInput>;
  columns?: boolean;
  unitLocked?: boolean;
}) {
  const t = useTranslations("catalog");
  const tu = useTranslations("units");
  const locale = useLocale();
  const { register, control, formState: { errors } } = form;
  const unit = useWatch({ control, name: "unit" });

  const basics = (
    <div className="space-y-6">
      <Field id="name" label={t("ingredientName")} placeholder={t("ingredientNamePlaceholder")} error={errors.name?.message} {...register("name")} />

      <Controller
        control={control}
        name="kind"
        render={({ field }) => (
          <fieldset aria-describedby="kind-hint">
            <legend className="text-sm font-semibold">{t("type")}</legend>
            <div className="mt-1.5 grid grid-cols-2 gap-1 rounded-field bg-base-200 p-1">
              {stockKinds.map((k) => (
                <label
                  key={k}
                  className="cursor-pointer rounded-field py-2 text-center text-sm text-base-content/70 has-checked:bg-white has-checked:font-semibold has-checked:text-base-content has-checked:shadow-sm has-focus-visible:outline-2"
                >
                  <input
                    type="radio"
                    name={field.name}
                    value={k}
                    checked={field.value === k}
                    onChange={() => field.onChange(k)}
                    className="sr-only"
                  />
                  {t(`kind.${k}`)}
                </label>
              ))}
            </div>
            <p id="kind-hint" className="mt-1 text-xs text-base-content/60">
              {t("kindHint")}
            </p>
          </fieldset>
        )}
      />

      <div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="unit" className="block text-sm font-semibold">{t("unit")}</label>
            {unitLocked ? (
              // Tidak didaftarkan ke form: select disabled membuat nilainya hilang dari data form.
              <select id="unit" disabled value={unit} aria-describedby="unit-locked" className="select mt-1.5 w-full border-base-300">
                <option value={unit}>{tu(`names.${unit}`)}</option>
              </select>
            ) : (
              <select id="unit" className="select mt-1.5 w-full border-base-300 bg-white/70" {...register("unit")}>
                {[...new Set(stockUnitKeys.map((u) => stockUnits[u].group))].map((group) => (
                  <optgroup key={group} label={tu(`groups.${group}`)}>
                    {stockUnitKeys.filter((u) => stockUnits[u].group === group).map((u) => (
                      <option key={u} value={u}>{tu(`names.${u}`)}</option>
                    ))}
                  </optgroup>
                ))}
              </select>
            )}
            {unitLocked && (
              <p id="unit-locked" className="mt-1 text-xs text-base-content/60">{t("unitLocked")}</p>
            )}
            {errors.unit && <p className="mt-1 text-xs text-error">{errors.unit.message}</p>}
          </div>
          <MoneyField
            id="unitCost"
            control={control}
            name="unitCost"
            // Harga perkiraan boleh kosong; kosong disimpan sebagai 0.
            emptyValue={0}
            label={t("estimatedPrice", { unit: unitLabel(unit, locale) })}
            error={errors.unitCost?.message}
          />
        </div>
        <p className="mt-2 text-xs text-base-content/60">
          {t("unitHint")}
        </p>
      </div>
    </div>
  );

  const stockRules = (
    <div className="space-y-6">
      <Controller
        control={control}
        name="shelfLifeDays"
        render={({ field }) => (
          <div>
            <label htmlFor="shelfLifeDays" className="block text-sm font-semibold">{t("shelfLife")}</label>
            <div className="mt-1.5 flex items-center rounded-field border border-base-300 bg-white/70 aria-invalid:border-error" aria-invalid={errors.shelfLifeDays ? true : undefined}>
              <button
                type="button"
                aria-label={t("dayLess")}
                disabled={field.value <= 1}
                onClick={() => field.onChange(field.value - 1)}
                className="btn btn-ghost btn-square"
              >
                <LuMinus className="size-4" aria-hidden />
              </button>
              <input
                id="shelfLifeDays"
                type="number"
                inputMode="numeric"
                min={1}
                max={365}
                value={Number.isNaN(field.value) ? "" : field.value}
                onChange={(e) => field.onChange(e.target.valueAsNumber)}
                onBlur={field.onBlur}
                aria-describedby={errors.shelfLifeDays ? "shelfLifeDays-error" : "shelfLifeDays-hint"}
                className="w-full bg-transparent py-2.5 text-center text-sm outline-none [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none"
              />
              <button
                type="button"
                aria-label={t("dayMore")}
                disabled={field.value >= 365}
                onClick={() => field.onChange((Number.isNaN(field.value) ? 0 : field.value) + 1)}
                className="btn btn-ghost btn-square"
              >
                <LuPlus className="size-4" aria-hidden />
              </button>
            </div>
            {errors.shelfLifeDays ? (
              <p id="shelfLifeDays-error" className="mt-1 text-xs text-error">{errors.shelfLifeDays.message}</p>
            ) : (
              <p id="shelfLifeDays-hint" className="mt-1 text-xs text-base-content/60">{t("shelfLifeHint")}</p>
            )}
            <div role="group" aria-label={t("shelfLifeShortcuts")} className="mt-2 flex flex-wrap gap-2">
              {shelfLifePresets.map(([days, key]) => (
                <button
                  key={days}
                  type="button"
                  aria-pressed={field.value === days}
                  onClick={() => field.onChange(days)}
                  className={`rounded-full border px-3 py-1 text-xs ${
                    field.value === days ? "border-base-content bg-base-content text-base-100" : "border-base-300 bg-white hover:bg-base-200"
                  }`}
                >
                  {t(`presets.${key}`)}
                </button>
              ))}
            </div>
          </div>
        )}
      />

      <ToggleCard id="carryOverAllowed" title={t("carryOverTitle")} text={t("carryOverText")} {...register("carryOverAllowed")} />
      <ToggleCard id="isPrimary" title={t("mainTitle")} text={t("mainText")} {...register("isPrimary")} />
    </div>
  );

  return columns ? (
    <div className="grid grid-cols-1 gap-8 md:grid-cols-2">{basics}{stockRules}</div>
  ) : (
    <div className="space-y-6">{basics}{stockRules}</div>
  );
}

function ToggleCard({ id, title, text, ...input }: { id: string; title: string; text: string } & ComponentProps<"input">) {
  return (
    <label htmlFor={id} className="flex cursor-pointer items-center justify-between gap-6 rounded-box border border-base-300 bg-white/50 px-5 py-4">
      <span>
        <span className="block text-sm font-semibold">{title}</span>
        <span id={`${id}-text`} className="mt-0.5 block text-xs text-base-content/60">{text}</span>
      </span>
      <input id={id} type="checkbox" role="switch" aria-describedby={`${id}-text`} className="toggle toggle-primary shrink-0" {...input} />
    </label>
  );
}
