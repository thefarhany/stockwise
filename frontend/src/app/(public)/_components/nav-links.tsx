"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { LuMenu, LuX } from "react-icons/lu";
import { sections } from "./sections";

const links = [{ href: "/", key: "home" } as const, ...sections];

function useLinks() {
  const pathname = usePathname();
  const t = useTranslations("public.nav");
  return links.map((s) => ({ href: s.href, label: t(s.key), active: s.href === pathname }));
}

export function NavLinks() {
  return (
    <ul className="hidden gap-8 text-sm md:flex">
      {useLinks().map((l) => (
        <li key={l.href}>
          <Link
            href={l.href}
            aria-current={l.active ? "page" : undefined}
            className={l.active ? "font-semibold" : "text-base-content/70 hover:text-base-content"}
          >
            {l.label}
          </Link>
        </li>
      ))}
    </ul>
  );
}

/** Di bawah md menu disembunyikan di balik tombol; panelnya turun di bawah header. Menutup sendiri saat tautan diklik. */
export function MobileNavLinks() {
  const [open, setOpen] = useState(false);
  const t = useTranslations("nav");
  const items = useLinks();
  return (
    <div className="md:hidden">
      <button
        type="button"
        className="btn btn-ghost btn-sm btn-square"
        aria-label={t("menu")}
        aria-expanded={open}
        aria-controls="public-menu"
        onClick={() => setOpen((o) => !o)}
      >
        {open ? <LuX className="size-5" aria-hidden /> : <LuMenu className="size-5" aria-hidden />}
      </button>
      {open && (
        <ul id="public-menu" className="absolute inset-x-0 top-full border-b border-base-300 bg-base-100 px-4 py-2 text-sm shadow-sm sm:px-6">
          {items.map((l) => (
            <li key={l.href}>
              <Link
                href={l.href}
                aria-current={l.active ? "page" : undefined}
                className={`block rounded-field px-2 py-3 ${l.active ? "font-semibold" : "text-base-content/70 hover:text-base-content"}`}
                onClick={() => setOpen(false)}
              >
                {l.label}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
