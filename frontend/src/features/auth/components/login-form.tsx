"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { Field } from "@/components/ui/field";
import { ApiError, errorMessage } from "@/lib/api-client";
import { login } from "../api/auth";
import { loginSchema, type LoginInput } from "../schemas";
import { FormError } from "./form-error";

export function LoginForm() {
  const router = useRouter();
  const t = useTranslations("auth");
  const tv = useTranslations("validation");
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema(tv)),
  });

  const onSubmit = handleSubmit(async (input) => {
    try {
      await login(input);
      router.replace("/dashboard");
    } catch (e) {
      setError("root", { message: e instanceof ApiError ? e.message : errorMessage("") });
    }
  });

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-6">
      <FormError message={errors.root?.message} />
      <Field id="email" label={t("email")} type="email" autoComplete="email" placeholder="sari@example.com" error={errors.email?.message} {...register("email")} />
      <Field id="password" label={t("password")} type="password" autoComplete="current-password" placeholder={t("yourPassword")} error={errors.password?.message} {...register("password")} />
      <button type="submit" disabled={isSubmitting} className="btn btn-primary w-full">
        {isSubmitting ? t("loggingIn") : t("logIn")}
      </button>
      <p className="text-center text-sm">
        {t("noAccount")} <Link href="/signup" className="ml-1 font-semibold underline">{t("signUpFree")}</Link>
      </p>
    </form>
  );
}
