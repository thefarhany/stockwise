"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { LuTriangleAlert, LuX } from "react-icons/lu";
import { Modal } from "@/components/ui/modal";
import { logout } from "@/features/auth";
import { deleteOutlet } from "@/features/onboarding/api/outlet";
import { ApiError, errorMessage } from "@/lib/api-client";

/** Zona bahaya: hapus usaha permanen, dikonfirmasi dengan mengetik nama usaha persis. */
export function DeleteOutletSection({
  name,
  counts,
}: {
  name: string;
  counts: { users: number; stockItems: number; products: number };
}) {
  const router = useRouter();
  const t = useTranslations("settings");
  const tc = useTranslations("common");
  const [open, setOpen] = useState(false);
  const [typed, setTyped] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const matches = typed === name;

  async function confirm() {
    if (!matches || pending) return;
    setPending(true);
    setError(null);
    try {
      await deleteOutlet(typed);
    } catch (e) {
      setPending(false);
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      if (!(e instanceof ApiError)) console.error(e);
      setError(e instanceof ApiError ? e.message : errorMessage(""));
      return;
    }
    // Akun ikut terhapus; bersihkan cookie sesi lalu keluar.
    await logout().catch(() => {});
    router.replace("/");
  }

  return (
    <section id="delete" aria-labelledby="delete-heading" className="scroll-mt-8 flex flex-wrap items-center justify-between gap-6 rounded-box border border-error/70 bg-white/60 px-7 py-6 md:flex-nowrap">
      <div className="min-w-0">
        <h2 id="delete-heading" className="text-xl font-semibold text-error">{t("deleteTitle")}</h2>
        <p className="mt-2 text-sm text-base-content/80">
          {t("deleteLead", { name })}
        </p>
      </div>
      <button type="button" className="btn btn-sm shrink-0 border-error bg-white text-error hover:bg-error/10" onClick={() => { setTyped(""); setError(null); setOpen(true); }}>
        {t("deleteTitle")}
      </button>

      <Modal open={open} onClose={() => !pending && setOpen(false)} labelledBy="delete-title" className="max-w-lg">
        <div className="flex items-start gap-4 border-b border-base-300 px-6 py-5">
          <span className="grid size-10 shrink-0 place-items-center rounded-full bg-base-200 text-error">
            <LuTriangleAlert className="size-5" aria-hidden />
          </span>
          <div className="flex-1">
            <h2 id="delete-title" className="text-xl font-semibold">{t("deleteConfirmTitle")}</h2>
            <p className="mt-1 text-sm text-base-content/70">{t("deleteConfirmLead", { name })}</p>
          </div>
          <button type="button" className="btn btn-ghost btn-sm btn-square" aria-label={tc("close")} disabled={pending} onClick={() => setOpen(false)}>
            <LuX className="size-5" aria-hidden />
          </button>
        </div>
        <div className="space-y-5 px-6 py-6">
          <div className="rounded-box bg-base-200/60 px-5 py-4 text-sm">
            <p className="text-xs uppercase tracking-widest text-base-content/70">{t("alsoDeleted")}</p>
            <ul className="mt-2 list-inside list-disc space-y-1">
              <li>{t("deletedAccounts", { count: counts.users })}</li>
              <li>{t("deletedCatalog", { ingredients: counts.stockItems, products: counts.products })}</li>
              <li>{t("deletedHistory")}</li>
            </ul>
          </div>
          <div>
            <label htmlFor="confirm-name" className="block text-sm font-semibold">{t("typeName")}</label>
            <input
              id="confirm-name"
              className="input mt-1.5 w-full"
              autoComplete="off"
              value={typed}
              onChange={(e) => setTyped(e.target.value)}
              aria-describedby="confirm-hint"
            />
            <p id="confirm-hint" className="mt-1 text-xs text-base-content/60">{t("mustMatch", { name })}</p>
          </div>
          {error && <p className="text-sm text-error" role="alert">{error}</p>}
        </div>
        <div className="flex justify-end gap-3 border-t border-base-300 px-6 py-4">
          <button type="button" className="btn border-base-300 bg-white" disabled={pending} onClick={() => setOpen(false)}>{tc("cancel")}</button>
          <button type="button" className="btn btn-error" disabled={!matches || pending} onClick={confirm}>
            {pending ? t("deleting") : t("deletePermanently")}
          </button>
        </div>
      </Modal>
    </section>
  );
}
