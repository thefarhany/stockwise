// Validasi form di sisi tampilan; batasnya sama dengan backend (dto/auth/RegisterRequest, LoginRequest).
// Skema dibuat per bahasa: berikan t = useTranslations("validation").
import { z } from "zod";
import type { ValidationT } from "@/i18n/validation";

const email = (t: ValidationT) =>
  z.string().trim().min(1, t("emailRequired")).max(150, t("maxChars", { max: 150 })).pipe(z.email(t("emailInvalid")));

export const loginSchema = (t: ValidationT) =>
  z.object({
    email: email(t),
    password: z.string().min(1, t("passwordRequired")),
  });

export const signupSchema = (t: ValidationT) =>
  z.object({
    name: z.string().trim().min(1, t("nameRequired")).max(100, t("maxChars", { max: 100 })),
    email: email(t),
    password: z.string().min(8, t("minChars", { min: 8 })).max(72, t("maxChars", { max: 72 })),
    outletName: z.string().trim().min(1, t("businessNameRequired")).max(100, t("maxChars", { max: 100 })),
  });

export type LoginInput = z.infer<ReturnType<typeof loginSchema>>;
export type SignupInput = z.infer<ReturnType<typeof signupSchema>>;

/** Akun kasir baru dari Settings: sama dengan pendaftaran, tanpa nama usaha. */
export const cashierSchema = (t: ValidationT) => signupSchema(t).omit({ outletName: true });
export const staffNameSchema = (t: ValidationT) => signupSchema(t).pick({ name: true });

export type CashierInput = z.infer<ReturnType<typeof cashierSchema>>;
