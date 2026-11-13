"use client";

import { useTranslations } from "next-intl";
import { type ReactNode, useEffect, useRef } from "react";

/** <dialog> modal bawaan browser (fokus terkunci, Esc menutup) yang dibuka/ditutup lewat `open`. */
export function Modal({
  open,
  onClose,
  labelledBy,
  className = "max-w-md",
  children,
}: {
  open: boolean;
  onClose: () => void;
  labelledBy: string;
  /** Kelas tambahan untuk kotak modal, mis. lebar. */
  className?: string;
  children: ReactNode;
}) {
  const t = useTranslations("common");
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (open && !d.open) {
      d.showModal();
      // showModal memfokuskan elemen pertama; `data-autofocus` menentukan tombol utama (mis. Enter = lanjut).
      d.querySelector<HTMLElement>("[data-autofocus]")?.focus();
    }
    if (!open && d.open) d.close();
  }, [open]);

  return (
    <dialog ref={ref} className="modal" aria-labelledby={labelledBy} onClose={onClose}>
      <div className={`modal-box p-0 ${className}`}>{open && children}</div>
      <form method="dialog" className="modal-backdrop">
        <button aria-label={t("close")}>{t("close")}</button>
      </form>
    </dialog>
  );
}
