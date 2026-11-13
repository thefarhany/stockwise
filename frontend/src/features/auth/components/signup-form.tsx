"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { toLanguage } from "@/i18n/locale";
import { useForm } from "react-hook-form";
import { Field } from "@/components/ui/field";
import { ApiError, errorMessage } from "@/lib/api-client";
import { signup } from "../api/auth";
import { signupSchema, type SignupInput } from "../schemas";
import { FormError } from "./form-error";

export function SignupForm() {
  const router = useRouter();
  const t = useTranslations("auth");
  const tv = useTranslations("validation");
  const locale = useLocale();
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<SignupInput>({
    resolver: zodResolver(signupSchema(tv)),
  });

  const onSubmit = handleSubmit(async (input) => {
    try {
      // Akun Owner mulai dengan bahasa yang sedang dipakai di halaman ini.
      await signup({ ...input, language: toLanguage(locale) });
      router.replace("/dashboard");
    } catch (e) {
      if (e instanceof ApiError && e.errorCode === "EMAIL_TAKEN") {
        setError("email", { message: e.message }, { shouldFocus: true });
      } else {
        setError("root", { message: e instanceof ApiError ? e.message : errorMessage("") });
      }
    }
  });

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-3">
      <FormError message={errors.root?.message} />
      <Field id="name" label={t("fullName")} autoComplete="name" placeholder="Sari Wulandari" hint={t("fullNameHint")} error={errors.name?.message} {...register("name")} />
      <Field id="email" label={t("email")} type="email" autoComplete="email" placeholder="sari@example.com" hint={t("emailHint")} error={errors.email?.message} {...register("email")} />
      <Field id="password" label={t("password")} type="password" autoComplete="new-password" placeholder={t("passwordPlaceholder")} hint={t("passwordHint")} error={errors.password?.message} {...register("password")} />
      <Field id="outletName" label={t("businessName")} autoComplete="organization" placeholder={t("businessNamePlaceholder")} hint={t("businessNameHint")} error={errors.outletName?.message} {...register("outletName")} />
      <p className="text-sm">
        {t.rich("agree", { link: (chunks) => <Link href="/privacy" className="underline">{chunks}</Link> })}
      </p>
      <button type="submit" disabled={isSubmitting} className="btn btn-primary w-full">
        {isSubmitting ? t("creatingAccount") : t("createAccount")}
      </button>
      <p className="text-center text-sm">
        {t("haveAccount")} <Link href="/login" className="ml-1 font-semibold underline">{t("logIn")}</Link>
      </p>
    </form>
  );
}
