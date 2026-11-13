import Link from "next/link";
import { useTranslations } from "next-intl";

export function CtaBand({ title, text }: { title: string; text: string }) {
  const t = useTranslations("public");
  return (
    <section className="bg-base-200">
      <div className="mx-auto flex max-w-6xl flex-col justify-between gap-6 px-6 py-16 md:flex-row md:items-center">
        <div>
          <h2 className="text-5xl font-semibold tracking-tight md:text-6xl">{title}</h2>
          <p className="mt-4 text-base-content/80">{text}</p>
        </div>
        <div className="flex gap-3">
          <Link href="/signup" className="btn btn-primary">{t("signUpFree")}</Link>
          <Link href="/login" className="btn border-base-300 bg-white">{t("logIn")}</Link>
        </div>
      </div>
    </section>
  );
}
