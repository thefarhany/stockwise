"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { LuCalendar, LuChevronDown } from "react-icons/lu";
import { useFormat } from "@/i18n/format";
import { dayBefore } from "@/lib/format";

/** Pilihan tanggal riwayat transaksi: Today, Yesterday, atau tanggal lain. Mengganti `?date=` di URL. */
export function DateFilter({ date, today }: { date: string; today: string }) {
  const router = useRouter();
  const t = useTranslations("dateFilter");
  const f = useFormat();
  const yesterday = dayBefore(today);
  const label = date === today ? t("today") : date === yesterday ? t("yesterday") : f.date(date);

  function pick(next: string) {
    // Dropdown daisyUI terbuka selama fokus ada di dalamnya; lepas fokus untuk menutupnya.
    (document.activeElement as HTMLElement | null)?.blur();
    // Halaman kembali ke 1 karena isinya tanggal lain.
    router.push(next === today ? "/transactions" : `/transactions?date=${next}`);
  }

  const option = (value: string, text: string) => (
    <li>
      <button type="button" aria-current={date === value} className={date === value ? "menu-active" : ""} onClick={() => pick(value)}>
        {text}
      </button>
    </li>
  );

  return (
    <div className="dropdown dropdown-end">
      <div tabIndex={0} role="button" aria-haspopup="true" className="btn border-base-300 bg-white font-normal">
        <LuCalendar className="size-4" aria-hidden />
        {label}
        <LuChevronDown className="size-4" aria-hidden />
      </div>
      <div tabIndex={-1} className="dropdown-content z-10 mt-2 w-64 rounded-box border border-base-300 bg-white p-2 shadow-lg">
        <ul className="menu w-full p-0">
          {option(today, t("today"))}
          {option(yesterday, t("yesterday"))}
        </ul>
        <label className="mt-2 block border-t border-base-300 px-3 pt-3 pb-1 text-sm">
          <span className="text-base-content/70">{t("otherDate")}</span>
          <input
            type="date"
            className="input input-sm mt-1 w-full"
            max={today}
            value={date}
            // Saat tahun diketik digit per digit muncul tanggal antara seperti 0202-…; tunggu sampai tahunnya masuk akal.
            onChange={(e) => e.target.value >= "2000" && pick(e.target.value)}
          />
        </label>
      </div>
    </div>
  );
}
