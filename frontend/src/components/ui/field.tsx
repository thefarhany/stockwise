"use client";

import { useState, type ComponentProps } from "react";
import { useTranslations } from "next-intl";
import { LuEye, LuEyeOff } from "react-icons/lu";

export type FieldProps = ComponentProps<"input"> & {
  id: string;
  label: string;
  hint?: string;
  error?: string;
  /** Satuan di ujung kiri input, mis. "Rp". */
  prefix?: string;
  /** Satuan di ujung kanan input, mis. "%". */
  suffix?: string;
};

const inputClass =
  "w-full rounded-field border border-base-300 bg-white/70 px-3.5 py-2.5 text-sm outline-none placeholder:text-base-content/50 focus:border-base-content aria-invalid:border-error";

/** Input berlabel dengan teks bantuan dan pesan error yang dibacakan screen reader. */
export function Field({ id, label, hint, error, prefix, suffix, type = "text", className, ...input }: FieldProps) {
  const t = useTranslations("common");
  const [shown, setShown] = useState(false);
  const isPassword = type === "password";
  const describedBy = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(" ") || undefined;

  return (
    <div className={className}>
      <label htmlFor={id} className="block text-sm font-semibold">{label}</label>
      <div className="relative mt-1.5">
        <input
          id={id}
          type={isPassword && shown ? "text" : type}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={`${inputClass} ${isPassword || suffix ? "pr-10" : ""} ${prefix ? "pl-11" : ""}`}
          {...input}
        />
        {prefix && (
          <span aria-hidden className="pointer-events-none absolute inset-y-0 left-0 grid w-11 place-items-center text-sm text-base-content/60">
            {prefix}
          </span>
        )}
        {suffix && (
          <span aria-hidden className="pointer-events-none absolute inset-y-0 right-0 grid w-10 place-items-center text-sm text-base-content/60">
            {suffix}
          </span>
        )}
        {isPassword && (
          <button
            type="button"
            onClick={() => setShown((s) => !s)}
            aria-label={shown ? t("hidePassword") : t("showPassword")}
            aria-pressed={shown}
            className="absolute inset-y-0 right-0 grid w-10 place-items-center text-base-content/60 hover:text-base-content"
          >
            {shown ? <LuEyeOff className="size-4" aria-hidden /> : <LuEye className="size-4" aria-hidden />}
          </button>
        )}
      </div>
      {hint && !error && <p id={`${id}-hint`} className="mt-1 text-xs text-base-content/60">{hint}</p>}
      {error && <p id={`${id}-error`} className="mt-1 text-xs text-error">{error}</p>}
    </div>
  );
}
