"use client";

import { useEffect, useRef, useState } from "react";
import { LuChevronDown } from "react-icons/lu";

const ITEM_HEIGHT = 32;
const MAX_HEIGHT = 288;

/**
 * Pilihan ringkas pengganti <select>, dengan daftar bergaya aplikasi. Daftar memakai position: fixed supaya tidak
 * terpotong wadah ber-overflow (mis. tabel yang bisa digeser), dan membuka ke atas bila ruang di bawah kurang.
 */
export function SelectMenu<T extends string>({
  value,
  options,
  onChange,
  label,
}: {
  value: T;
  options: { value: T; label: string }[];
  onChange: (value: T) => void;
  /** Nama untuk pembaca layar, mis. "Unit for Gula". */
  label: string;
}) {
  const [anchor, setAnchor] = useState<DOMRect | null>(null);
  const button = useRef<HTMLButtonElement>(null);
  const menu = useRef<HTMLUListElement>(null);
  const current = options.find((o) => o.value === value);

  const open = anchor !== null;
  useEffect(() => {
    if (!open) return;
    menu.current?.querySelector<HTMLElement>('[aria-pressed="true"]')?.focus({ preventScroll: true });
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== "Escape") return;
      setAnchor(null);
      button.current?.focus();
    };
    // Daftar ber-position fixed: ikuti tombol saat halaman/tabel digeser atau layar berubah ukuran.
    // Scroll di dalam daftar sendiri (daftar panjang) diabaikan.
    const follow = (e: Event) => {
      if (button.current && !menu.current?.contains(e.target as Node)) setAnchor(button.current.getBoundingClientRect());
    };
    document.addEventListener("keydown", onKey);
    window.addEventListener("scroll", follow, true);
    window.addEventListener("resize", follow);
    return () => {
      document.removeEventListener("keydown", onKey);
      window.removeEventListener("scroll", follow, true);
      window.removeEventListener("resize", follow);
    };
  }, [open]);

  function pick(next: T) {
    onChange(next);
    setAnchor(null);
    button.current?.focus();
  }

  const height = Math.min(options.length * ITEM_HEIGHT + 10, MAX_HEIGHT);
  const openUp = anchor !== null && anchor.bottom + height + 8 > window.innerHeight && anchor.top > height;

  return (
    <div
      className="shrink-0"
      // Fokus keluar dari tombol dan daftar (klik di luar, Tab keluar) menutup daftar.
      onBlur={(e) => !e.currentTarget.contains(e.relatedTarget) && setAnchor(null)}
    >
      <button
        ref={button}
        type="button"
        aria-label={label}
        aria-haspopup="true"
        aria-expanded={open}
        className="flex items-center gap-1 py-1.5 pr-2.5 pl-1 text-xs text-base-content/70 outline-none hover:text-base-content focus-visible:text-base-content"
        onClick={() => setAnchor(anchor ? null : button.current!.getBoundingClientRect())}
      >
        {current?.label ?? value}
        <LuChevronDown aria-hidden className={`size-3.5 transition-transform ${anchor ? "rotate-180" : ""}`} />
      </button>
      {anchor && (
        <ul
          ref={menu}
          className="fixed z-50 max-h-72 min-w-24 overflow-y-auto rounded-box border border-base-300 bg-white p-1 text-sm shadow-lg"
          style={{
            minWidth: anchor.width,
            left: anchor.right,
            translate: "-100% 0",
            ...(openUp ? { bottom: window.innerHeight - anchor.top + 4 } : { top: anchor.bottom + 4 }),
          }}
        >
          {options.map((o) => (
            <li key={o.value}>
              <button
                type="button"
                aria-pressed={o.value === value}
                className={`block h-8 w-full rounded-field px-3 text-left hover:bg-base-200 focus-visible:bg-base-200 focus-visible:outline-none ${o.value === value ? "bg-base-200 font-semibold" : ""}`}
                onClick={() => pick(o.value)}
              >
                {o.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
