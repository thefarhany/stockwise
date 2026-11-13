"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState, useTransition } from "react";
import { useForm } from "react-hook-form";
import { LuX } from "react-icons/lu";
import { Field } from "@/components/ui/field";
import { Modal } from "@/components/ui/modal";
import { createCashier, updateUser } from "@/features/auth/api/users";
import { FormError } from "@/features/auth/components/form-error";
import { cashierSchema, staffNameSchema, type CashierInput } from "@/features/auth/schemas";
import type { User } from "@/features/auth/types";
import { ApiError, errorMessage } from "@/lib/api-client";

const initials = (name: string) =>
  name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]!.toUpperCase()).join("");

function errorText(e: unknown) {
  if (!(e instanceof ApiError)) console.error(e);
  return e instanceof ApiError ? e.message : errorMessage("");
}

/** Daftar akun staf: tambah kasir, ganti nama, nonaktifkan/aktifkan. */
export function StaffSection({ users, meId }: { users: User[]; meId: string }) {
  const router = useRouter();
  const t = useTranslations("staff");
  const tc = useTranslations("common");
  const [adding, setAdding] = useState(false);
  const [renaming, setRenaming] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pendingId, setPendingId] = useState<string | null>(null);
  // Tombol tetap terkunci sampai daftar selesai dimuat ulang, supaya klik kedua tidak membalik status.
  const [refreshing, startRefresh] = useTransition();
  // Owner dulu, lalu kasir urut nama (backend sudah urut nama).
  const rows = users.toSorted((a, b) => Number(b.role === "OWNER") - Number(a.role === "OWNER"));

  async function setActive(u: User, active: boolean) {
    setError(null);
    setPendingId(u.id);
    try {
      await updateUser(u.id, { active });
      startRefresh(() => router.refresh());
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      setError(errorText(e));
    } finally {
      setPendingId(null);
    }
  }

  return (
    <section id="staff" aria-labelledby="staff-heading" className="scroll-mt-8 overflow-hidden rounded-box border border-base-300 bg-white/60">
      <div className="flex flex-wrap items-start justify-between gap-4 px-7 py-6">
        <div>
          <h2 id="staff-heading" className="text-xl font-semibold">{t("title")}</h2>
          <p className="mt-1 text-sm text-base-content/70">
            {t("lead")}
          </p>
        </div>
        <button type="button" className="btn btn-sm border-base-300 bg-white" onClick={() => setAdding(true)}>{t("addCashier")}</button>
      </div>
      {error && <p className="px-7 pb-3 text-sm text-error" role="alert">{error}</p>}
      <div className="relative overflow-x-auto">
        <table className="w-full min-w-2xl text-left text-sm">
          <thead className="border-y border-base-300 bg-base-200/40 text-xs uppercase tracking-widest text-base-content/70">
            <tr>
              <th className="px-7 py-3 font-normal">{t("colName")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colRole")}</th>
              <th className="px-3 py-3 text-center font-normal">{t("colStatus")}</th>
              <th className="px-7 py-3"><span className="sr-only">{t("colActions")}</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-base-300">
            {rows.map((u) => (
              <tr key={u.id} className={u.active ? "" : "text-base-content/60"}>
                <td className="px-7 py-3">
                  <span className="flex items-center gap-3">
                    <span aria-hidden className="grid size-9 shrink-0 place-items-center rounded-full bg-base-200 text-xs">{initials(u.name)}</span>
                    <span>
                      <span className="block">{u.name}</span>
                      <span className="text-xs text-base-content/60">{u.email}</span>
                    </span>
                  </span>
                </td>
                <td className="px-3 py-3 text-center">{u.role === "OWNER" ? t("owner") : t("cashier")}</td>
                <td className="px-3 py-3 text-center">
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-base-200 px-2.5 py-0.5 text-xs">
                    <span aria-hidden className={`size-1.5 rounded-full ${u.active ? "bg-success" : "bg-base-content/40"}`} />
                    {u.active ? t("active") : t("inactive")}
                  </span>
                </td>
                <td className="px-7 py-3 text-center">
                  {u.id === meId ? (
                    <span className="text-base-content/60">{tc("you")}</span>
                  ) : (
                    <span className="inline-flex items-center gap-2">
                      <button type="button" className="btn btn-sm border-base-300 bg-white font-normal" aria-label={t("editName", { name: u.name })} onClick={() => setRenaming(u)}>{tc("edit")}</button>
                      <button
                        type="button"
                        className="btn btn-sm border-base-300 bg-white font-normal"
                        disabled={pendingId !== null || refreshing}
                        onClick={() => setActive(u, !u.active)}
                      >
                        {u.active ? t("deactivate") : t("activate")}
                        <span className="sr-only"> {u.name}</span>
                      </button>
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="border-t border-base-300 bg-base-200/40 px-7 py-3 text-xs text-base-content/70">
        {t("note")}
      </p>

      <Modal open={adding} onClose={() => setAdding(false)} labelledBy="add-cashier-title">
        {adding && <AddCashierDialog onDone={() => { setAdding(false); router.refresh(); }} onCancel={() => setAdding(false)} />}
      </Modal>
      <Modal open={renaming !== null} onClose={() => setRenaming(null)} labelledBy="rename-title">
        {renaming && (
          <RenameDialog key={renaming.id} user={renaming} onDone={() => { setRenaming(null); router.refresh(); }} onCancel={() => setRenaming(null)} />
        )}
      </Modal>
    </section>
  );
}

function DialogHeader({ id, title, lead, onClose }: { id: string; title: string; lead?: string; onClose: () => void }) {
  const tc = useTranslations("common");
  return (
    <div className="flex items-start justify-between gap-4 border-b border-base-300 px-6 py-5">
      <div>
        <h2 id={id} className="text-xl font-semibold">{title}</h2>
        {lead && <p className="mt-1 text-sm text-base-content/70">{lead}</p>}
      </div>
      <button type="button" className="btn btn-ghost btn-sm btn-square" aria-label={tc("close")} onClick={onClose}>
        <LuX className="size-5" aria-hidden />
      </button>
    </div>
  );
}

function AddCashierDialog({ onDone, onCancel }: { onDone: () => void; onCancel: () => void }) {
  const router = useRouter();
  const t = useTranslations("staff");
  const tc = useTranslations("common");
  const tv = useTranslations("validation");
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<CashierInput>({
    resolver: zodResolver(cashierSchema(tv)),
    defaultValues: { name: "", email: "", password: "" },
  });
  const save = handleSubmit(async (input) => {
    try {
      await createCashier(input);
      onDone();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      if (e instanceof ApiError && e.errorCode === "EMAIL_TAKEN") {
        setError("email", { message: t("emailTaken") }, { shouldFocus: true });
      } else setError("root", { message: errorText(e) });
    }
  });

  return (
    <form onSubmit={save} noValidate>
      <DialogHeader id="add-cashier-title" title={t("addCashier")} lead={t("addLead")} onClose={onCancel} />
      <div className="space-y-5 px-6 py-6">
        <FormError message={errors.root?.message} />
        <Field id="cashier-name" label={t("name")} placeholder={t("namePlaceholder")} autoComplete="off" error={errors.name?.message} {...register("name")} />
        <Field id="cashier-email" label={t("email")} type="email" placeholder={t("emailPlaceholder")} autoComplete="off" hint={t("emailHint")} error={errors.email?.message} {...register("email")} />
        <Field id="cashier-password" label={t("password")} type="password" placeholder={t("passwordPlaceholder")} autoComplete="new-password" hint={t("passwordHint")} error={errors.password?.message} {...register("password")} />
      </div>
      <div className="flex justify-end gap-3 border-t border-base-300 px-6 py-4">
        <button type="button" className="btn border-base-300 bg-white" onClick={onCancel}>{tc("cancel")}</button>
        <button type="submit" className="btn btn-primary" disabled={isSubmitting}>{isSubmitting ? t("adding") : t("addCashier")}</button>
      </div>
    </form>
  );
}

function RenameDialog({ user, onDone, onCancel }: { user: User; onDone: () => void; onCancel: () => void }) {
  const router = useRouter();
  const t = useTranslations("staff");
  const tc = useTranslations("common");
  const tv = useTranslations("validation");
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<{ name: string }>({
    resolver: zodResolver(staffNameSchema(tv)),
    defaultValues: { name: user.name },
  });
  const save = handleSubmit(async ({ name }) => {
    try {
      await updateUser(user.id, { name });
      onDone();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      setError("root", { message: errorText(e) });
    }
  });

  return (
    <form onSubmit={save} noValidate>
      <DialogHeader id="rename-title" title={t("editCashier")} lead={user.email} onClose={onCancel} />
      <div className="space-y-5 px-6 py-6">
        <FormError message={errors.root?.message} />
        <Field id="staff-name" label={t("name")} autoComplete="off" error={errors.name?.message} {...register("name")} />
      </div>
      <div className="flex justify-end gap-3 border-t border-base-300 px-6 py-4">
        <button type="button" className="btn border-base-300 bg-white" onClick={onCancel}>{tc("cancel")}</button>
        <button type="submit" className="btn btn-primary" disabled={isSubmitting}>{isSubmitting ? tc("saving") : tc("save")}</button>
      </div>
    </form>
  );
}
