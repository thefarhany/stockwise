import Link from "next/link";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import { LuArrowLeft } from "react-icons/lu";
import { LanguageSelect } from "@/components/language-select";

/** Kerangka halaman login/daftar: form di kiri, panel gelap di kanan (disembunyikan di HP). */
export function AuthShell({ children, aside }: { children: ReactNode; aside: ReactNode }) {
  const t = useTranslations("common");
  return (
    <div className="grid grid-cols-1 min-h-dvh flex-1 lg:grid-cols-2">
      <div className="flex flex-col px-6 py-5 sm:px-12 lg:px-14">
        <header className="flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2 text-lg font-semibold">
            <span className="grid size-7 place-items-center rounded-md bg-base-content text-xs text-base-100">S</span>
            Stockwise
          </Link>
          <div className="flex items-center gap-4">
            <LanguageSelect scope="browser" compact />
            <Link href="/" className="flex items-center gap-2 text-sm text-base-content/80 hover:text-base-content">
              <LuArrowLeft aria-hidden className="size-4" /> {t("home")}
            </Link>
          </div>
        </header>

        <main className="mx-auto my-auto w-full max-w-md py-3">{children}</main>

        <footer className="flex items-center justify-between text-xs text-base-content/70">
          <p>© 2026 Stockwise</p>
          <Link href="/privacy" className="underline hover:text-base-content">{t("privacyPolicy")}</Link>
        </footer>
      </div>

      <aside className="relative hidden overflow-hidden bg-neutral px-14 py-12 text-neutral-content lg:flex lg:flex-col">
        {aside}
        <p aria-hidden className="pointer-events-none absolute bottom-6 left-14 select-none whitespace-nowrap text-8xl font-bold tracking-tight text-neutral-content/5 xl:text-9xl">
          Stockwise
        </p>
      </aside>
    </div>
  );
}
