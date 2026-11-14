"use client";

import { useEffect, useState, type ReactNode } from "react";
import { LuEllipsis } from "react-icons/lu";

/**
 * Di bawah lg tampil seperti aplikasi HP: bar atas tipis, navigasi bawah (`tabs`) dan tombol "Lainnya" yang membuka
 * lembar berisi sisa sidebar (usaha, menu kelola, akun). Di lg ke atas isi sidebar selalu tampil, tanpa navigasi bawah.
 */
export function MobileNav({
  brand,
  tabs,
  moreLabel,
  moreActive,
  children,
}: {
  brand: ReactNode;
  tabs: ReactNode;
  moreLabel: string;
  /** Halaman aktif ada di lembar "Lainnya", bukan di tab. */
  moreActive: boolean;
  children: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  return (
    <>
      {brand}
      {open && <div aria-hidden className="fixed inset-0 z-30 bg-base-content/30 lg:hidden" onClick={() => setOpen(false)} />}
      <div
        id="app-sidebar"
        className={`${open ? "flex" : "hidden"} max-lg:fixed max-lg:inset-x-0 max-lg:bottom-16 max-lg:z-30 max-lg:max-h-[75dvh] max-lg:overflow-y-auto max-lg:rounded-t-box max-lg:border-t max-lg:border-base-300 max-lg:bg-base-100 max-lg:p-5 flex-1 flex-col gap-6 lg:flex`}
        onClick={(e) => (e.target as HTMLElement).closest("a") && setOpen(false)}
      >
        {children}
      </div>
      <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-base-300 bg-base-100 pb-[env(safe-area-inset-bottom)] lg:hidden">
        <ul className="flex h-16" onClick={(e) => (e.target as HTMLElement).closest("a") && setOpen(false)}>
          {tabs}
          <li className="flex-1">
            <button
              type="button"
              aria-expanded={open}
              aria-controls="app-sidebar"
              onClick={() => setOpen((o) => !o)}
              className={`flex h-full w-full flex-col items-center justify-center gap-1 text-[11px] ${
                open || moreActive ? "font-semibold text-base-content" : "text-base-content/60"
              }`}
            >
              <LuEllipsis className="size-5" aria-hidden />
              {moreLabel}
            </button>
          </li>
        </ul>
      </nav>
    </>
  );
}
