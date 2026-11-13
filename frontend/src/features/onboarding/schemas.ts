// Validasi form profil usaha; batasnya sama dengan backend (dto/outlet/UpdateOutletRequest, OutletService).
// Skema dibuat per bahasa: berikan t = useTranslations("validation").
import { z } from "zod";
import type { ValidationT } from "@/i18n/validation";
import { currencyCodes, type Currency } from "@/lib/currency";

export const profileSchema = (t: ValidationT) => {
  const time = z.string().regex(/^\d{2}:\d{2}$/, t("pickTime"));
  const percent = (max: number) => {
    const message = t("numberRange", { max });
    return z.number({ error: message }).min(0, message).max(max, message);
  };
  const text = (max: number) => z.string().trim().max(max, t("maxChars", { max }));
  return z
    .object({
      name: z.string().trim().min(1, t("businessNameRequired")).max(100, t("maxChars", { max: 100 })),
      currency: z.enum(currencyCodes as [Currency, ...Currency[]]),
      openTime: time,
      closeTime: time,
      latitude: z.number().nullable(),
      longitude: z.number().nullable(),
      // Batas sama dengan backend dto/outlet/AddressDto; teks kosong disimpan sebagai null.
      address: z.object({
        full: text(255),
        street: text(150),
        village: text(100),
        district: text(100),
        city: text(100),
        province: text(100),
        postalCode: text(10),
      }),
      paydayDays: z.array(z.number().int().min(1).max(31)).max(31),
      recommendationBufferPct: percent(100),
      targetMarginPct: percent(90),
    })
    // "HH:mm" bisa dibandingkan sebagai teks.
    .refine((v) => v.closeTime > v.openTime, { path: ["closeTime"], message: t("closeAfterOpen") });
};

export type ProfileInput = z.infer<ReturnType<typeof profileSchema>>;
