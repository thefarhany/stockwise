"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { type ReactNode, useState } from "react";
import { logout } from "@/features/auth";

export function LogoutButton({
  className = "btn border-base-300 bg-white",
  children,
}: {
  className?: string;
  /** Isi tombol pengganti teks "Log out", mis. ikon; label aksesibel tetap "Log out". */
  children?: ReactNode;
}) {
  const t = useTranslations("common");
  const router = useRouter();
  const [pending, setPending] = useState(false);
  return (
    <button
      type="button"
      disabled={pending}
      className={className}
      aria-label={children ? t("logOut") : undefined}
      onClick={async () => {
        setPending(true);
        // Cookie dihapus BFF; kalau gagal pun pengguna tetap diarahkan keluar.
        await logout().catch(() => {});
        router.replace("/login");
      }}
    >
      {children ?? (pending ? t("loggingOut") : t("logOut"))}
    </button>
  );
}
