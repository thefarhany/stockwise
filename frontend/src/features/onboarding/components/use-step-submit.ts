"use client";

import { useRouter } from "next/navigation";
import type { FormEvent } from "react";
import { logout } from "@/features/auth";
import { ApiError, errorMessage } from "@/lib/api-client";
import { EXIT_INTENT } from "../constants";

/** value tombol "Save, add another". */
export const ADD_MORE = "more";

/** value tombol yang men-submit form; tombol lanjut tidak punya value. */
export const submitIntent = (e?: { nativeEvent: object }) =>
  ((e?.nativeEvent as SubmitEvent | undefined)?.submitter as HTMLButtonElement | null)?.value || undefined;

/** Alur bersama langkah yang bisa menambah banyak data (bahan, produk): tambah lagi, lanjut, atau keluar. */
export function useStepSubmit({ next, hasItems, isEmpty }: { next: string; hasItems: boolean; isEmpty: () => boolean }) {
  const router = useRouter();
  const exit = async () => {
    await logout().catch(() => {});
    router.replace("/login");
  };

  return {
    /** Dipanggil sebelum validasi. Form kosong tidak perlu disimpan untuk keluar, atau untuk lanjut bila sudah ada data. */
    skip(e: FormEvent<HTMLFormElement>) {
      const intent = submitIntent(e);
      if (intent === ADD_MORE || !isEmpty()) return false;
      if (intent !== EXIT_INTENT && !hasItems) return false;
      e.preventDefault();
      if (intent === EXIT_INTENT) void exit();
      else router.push(next);
      return true;
    },
    /** Setelah berhasil simpan. true = tetap di halaman, form perlu dikosongkan. */
    saved(intent?: string) {
      if (intent === EXIT_INTENT) void exit();
      else if (intent !== ADD_MORE) router.push(next);
      else {
        // Muat ulang daftar di panel kanan (server component).
        router.refresh();
        return true;
      }
      return false;
    },
    /** Pesan untuk ditampilkan, atau null bila sesi habis dan pengguna sudah diarahkan ke /login. */
    failed(e: unknown) {
      if (e instanceof ApiError && e.status === 401) {
        router.replace("/login");
        return null;
      }
      return e instanceof ApiError ? e.message : errorMessage("");
    },
  };
}
