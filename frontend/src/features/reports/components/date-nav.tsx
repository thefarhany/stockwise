"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { LuCalendar, LuChevronLeft, LuChevronRight } from "react-icons/lu";

const shift = (date: string, days: number) =>
  new Date(Date.parse(`${date}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);

/** ‹ tanggal › untuk laporan harian; tidak bisa maju melewati hari ini. */
export function DateNav({ date, today }: { date: string; today: string }) {
  const router = useRouter();
  const t = useTranslations("dateNav");
  const locale = useLocale();
  const go = (d: string) => `/reports?date=${d}` as const;
  const label = new Date(`${date}T00:00:00Z`).toLocaleDateString(locale === "en" ? "en-GB" : locale, {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  });
  const next = shift(date, 1);
  const arrow = "btn btn-ghost btn-sm btn-square";

  return (
    <div className="flex items-center rounded-field border border-base-300 bg-white">
      <Link href={go(shift(date, -1))} className={arrow} aria-label={t("previousDay")}>
        <LuChevronLeft className="size-4" aria-hidden />
      </Link>
      <label className="relative flex cursor-pointer items-center gap-2 border-x border-base-300 px-3 py-1.5 text-sm">
        <LuCalendar className="size-4" aria-hidden />
        {label}
        {/* Input tanggal transparan di atas label: klik membuka pemilih tanggal bawaan browser. */}
        <input
          type="date"
          aria-label={t("reportDate")}
          className="absolute inset-0 cursor-pointer opacity-0"
          max={today}
          value={date}
          // Saat tahun diketik digit per digit muncul tanggal antara seperti 0202-…; tunggu sampai tahunnya masuk akal.
          onChange={(e) => e.target.value >= "2000" && router.push(go(e.target.value))}
        />
      </label>
      {next <= today ? (
        <Link href={go(next)} className={arrow} aria-label={t("nextDay")}>
          <LuChevronRight className="size-4" aria-hidden />
        </Link>
      ) : (
        <span aria-disabled="true" className={`${arrow} btn-disabled`}>
          <LuChevronRight className="size-4" aria-hidden />
        </span>
      )}
    </div>
  );
}
