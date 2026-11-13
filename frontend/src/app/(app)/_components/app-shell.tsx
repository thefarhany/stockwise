import Link from "next/link";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import type { IconType } from "react-icons";
import {
  LuBookOpen,
  LuChartColumn,
  LuCircleHelp,
  LuHouse,
  LuLogOut,
  LuMonitor,
  LuPackage,
  LuReceiptText,
  LuSettings,
} from "react-icons/lu";
import { CurrencyProvider } from "@/components/currency";
import { LogoutButton } from "@/components/logout-button";
import type { Profile } from "@/features/auth/types";
import { hhmm } from "@/lib/format";
import { MobileNav } from "./mobile-nav";

type NavKey = "home" | "cashier" | "transactions" | "reports" | "ingredients" | "products" | "settings" | "docs";

type NavItem = {
  key: NavKey;
  icon: IconType;
  href?: "/dashboard" | "/cashier" | "/transactions" | "/reports" | "/ingredients" | "/products" | "/settings" | "/docs";
  /** Hanya untuk Owner (endpoint-nya OWNER). */
  ownerOnly?: true;
};

const mainNav: NavItem[] = [
  { key: "home", icon: LuHouse, href: "/dashboard" },
  { key: "cashier", icon: LuMonitor, href: "/cashier" },
  { key: "transactions", icon: LuReceiptText, href: "/transactions" },
  { key: "reports", icon: LuChartColumn, href: "/reports", ownerOnly: true },
];

const manageNav: NavItem[] = [
  { key: "ingredients", icon: LuPackage, href: "/ingredients", ownerOnly: true },
  { key: "products", icon: LuBookOpen, href: "/products", ownerOnly: true },
  { key: "settings", icon: LuSettings, href: "/settings" },
  { key: "docs", icon: LuCircleHelp, href: "/docs" },
];

const initials = (name: string) =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]!.toUpperCase())
    .join("");

/** Kerangka layar aplikasi: sidebar (usaha, menu, akun) di kiri, isi di kanan. */
export function AppShell({
  profile,
  active,
  children,
  bare = false,
}: {
  profile: Profile;
  /** Menu yang sedang dibuka. */
  active: NavKey;
  children: ReactNode;
  /** Isi tanpa padding, untuk layar yang mengatur tepinya sendiri (mis. kasir dengan panel keranjang). */
  bare?: boolean;
}) {
  const { user, outlet } = profile;
  const t = useTranslations("nav");
  const navList = (items: NavItem[]) => (
    <ul className="flex flex-col gap-1">
      {items.filter((i) => !i.ownerOnly || user.role === "OWNER").map(({ key, icon: Icon, href }) => {
        const content = (
          <>
            <Icon className="size-5 shrink-0" aria-hidden />
            {t(key)}
          </>
        );
        const base = "flex shrink-0 items-center gap-3 rounded-field px-3 py-2.5 text-sm";
        return (
          <li key={key}>
            {href ? (
              <Link
                href={href}
                aria-current={key === active ? "page" : undefined}
                className={`${base} ${key === active ? "bg-base-200 font-medium" : "hover:bg-base-200/60"}`}
              >
                {content}
              </Link>
            ) : (
              <span aria-disabled="true" title={t("comingSoon")} className={`${base} cursor-not-allowed text-base-content/50`}>
                {content}
              </span>
            )}
          </li>
        );
      })}
    </ul>
  );

  return (
    <div className="flex flex-1 flex-col lg:flex-row">
      <aside className="flex flex-col gap-6 border-b border-base-300 bg-white/40 px-4 py-3 lg:sticky lg:p-5 lg:top-0 lg:h-screen lg:w-72 lg:shrink-0 lg:border-r lg:border-b-0">
        <MobileNav
          label={t("menu")}
          brand={
            <Link href="/dashboard" className="flex items-center gap-3 px-1 text-xl font-semibold">
              <span className="grid size-8 place-items-center rounded-lg bg-base-content text-sm text-base-100">S</span>
              Stockwise
            </Link>
          }
        >
          <div className="rounded-box border border-base-300 bg-white/60 px-4 py-3">
            <p className="truncate text-sm font-semibold">{outlet.name}</p>
            <p className="text-xs text-base-content/70">
              {t("openHours", { open: hhmm(outlet.openTime), close: hhmm(outlet.closeTime) })}
            </p>
          </div>

          <nav aria-label={t("main")} className="flex flex-col gap-6">
            {navList(mainNav)}
            <div>
              <p className="mb-2 px-3 text-xs uppercase tracking-widest text-base-content/70">{t("manage")}</p>
              {navList(manageNav)}
            </div>
          </nav>

          <div className="mt-auto space-y-3 border-t border-base-300 pt-4">
            <div className="flex items-center gap-3">
              <span className="grid size-10 shrink-0 place-items-center rounded-full bg-base-200 text-sm" aria-hidden>
                {initials(user.name)}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm">{user.name}</span>
                <span className="text-xs text-base-content/70">{user.role === "OWNER" ? t("owner") : t("cashierRole")}</span>
              </span>
              <LogoutButton className="btn btn-ghost btn-sm btn-square">
                <LuLogOut className="size-5" aria-hidden />
              </LogoutButton>
            </div>
          </div>
        </MobileNav>
      </aside>

      <main className={`min-w-0 flex-1 ${bare ? "" : "px-4 py-8 sm:px-6 lg:px-16 lg:py-10"}`}>
        <CurrencyProvider currency={outlet.currency}>{children}</CurrencyProvider>
      </main>
    </div>
  );
}
