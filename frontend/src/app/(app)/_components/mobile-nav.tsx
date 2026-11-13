"use client";

import { useState, type ReactNode } from "react";
import { LuMenu, LuX } from "react-icons/lu";

/**
 * Di bawah lg: isi sidebar (usaha, menu, akun) disembunyikan di balik tombol menu supaya halaman tidak terdorong ke
 * bawah. Di lg ke atas selalu tampil. Menutup sendiri saat salah satu tautan diklik.
 */
export function MobileNav({ brand, label, children }: { brand: ReactNode; label: string; children: ReactNode }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <div className="flex items-center justify-between">
        {brand}
        <button
          type="button"
          className="btn btn-ghost btn-sm btn-square lg:hidden"
          aria-label={label}
          aria-expanded={open}
          aria-controls="app-sidebar"
          onClick={() => setOpen((o) => !o)}
        >
          {open ? <LuX className="size-5" aria-hidden /> : <LuMenu className="size-5" aria-hidden />}
        </button>
      </div>
      <div
        id="app-sidebar"
        className={`${open ? "flex" : "hidden"} flex-1 flex-col gap-6 lg:flex`}
        onClick={(e) => (e.target as HTMLElement).closest("a") && setOpen(false)}
      >
        {children}
      </div>
    </>
  );
}
