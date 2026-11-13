import type { Metadata } from "next";
import Link from "next/link";
import { getTranslations } from "next-intl/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: `${(await getTranslations("notFound"))("meta")} · Stockwise` };
}

export default async function NotFound() {
  const t = await getTranslations("notFound");
  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col px-6 py-10">
      <Link href="/" className="flex w-fit items-center gap-2 font-semibold">
        <span className="grid size-6 place-items-center rounded-md bg-base-content text-xs text-base-100">S</span>
        Stockwise
      </Link>

      <div className="my-auto py-16">
        <p className="text-xs uppercase tracking-widest">{t("eyebrow")}</p>
        <h1 className="mt-4 text-5xl font-semibold leading-[1.05] tracking-tight md:text-6xl">{t("title")}</h1>
        <p className="mt-6 max-w-md text-base-content/80">{t("text")}</p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link href="/" className="btn btn-primary">{t("home")}</Link>
          <Link href="/how-it-works" className="btn border-base-300 bg-white">{t("how")}</Link>
        </div>
      </div>

      <p aria-hidden className="pointer-events-none select-none text-[28vw] font-bold leading-none tracking-tight text-base-content/5 md:text-[16rem]">
        404
      </p>
    </main>
  );
}
