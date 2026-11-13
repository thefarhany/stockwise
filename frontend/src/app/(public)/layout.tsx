import Link from "next/link";
import { useTranslations } from "next-intl";
import { FaGithub, FaInstagram, FaThreads, FaWhatsapp, FaXTwitter, FaYoutube } from "react-icons/fa6";
import { LanguageSelect } from "@/components/language-select";
import { MobileNavLinks, NavLinks } from "./_components/nav-links";
import { sections } from "./_components/sections";

// shortcut: URL akun sosial belum ada, ganti "#" sebelum halaman dipublikasikan
const socials = [
  { label: "X", href: "#", Icon: FaXTwitter },
  { label: "Instagram", href: "#", Icon: FaInstagram },
  { label: "Threads", href: "#", Icon: FaThreads },
  { label: "YouTube", href: "#", Icon: FaYoutube },
  { label: "WhatsApp", href: "#", Icon: FaWhatsapp },
  { label: "GitHub", href: "#", Icon: FaGithub },
];

function Logo({ badgeClass }: { badgeClass: string }) {
  return (
    <Link href="/" className="flex items-center gap-2 font-semibold">
      <span className={`grid size-6 place-items-center rounded-md text-xs ${badgeClass}`}>S</span>
      Stockwise
    </Link>
  );
}

export default function PublicLayout({ children }: LayoutProps<"/">) {
  const t = useTranslations("public");
  return (
    <>
      <header id="top" className="sticky top-0 z-10 border-b border-base-300 bg-base-100/90 backdrop-blur">
        <nav className="relative mx-auto flex h-16 max-w-6xl items-center justify-between gap-2 px-4 sm:gap-4 sm:px-6">
          <Logo badgeClass="bg-base-content text-base-100" />
          <NavLinks />
          <div className="flex items-center gap-2">
            <Link href="/login" className="btn btn-sm border-base-300 bg-white">{t("logIn")}</Link>
            <Link href="/signup" className="btn btn-sm btn-primary">{t("signUp")}</Link>
            <MobileNavLinks />
          </div>
        </nav>
      </header>

      {children}

      <footer className="overflow-hidden bg-neutral text-neutral-content">
        <div className="mx-auto max-w-6xl px-6 pt-14">
          <div className="flex flex-col justify-between gap-10 md:flex-row">
            <div className="max-w-sm">
              <Logo badgeClass="bg-neutral-content text-neutral" />
              <p className="mt-4 text-sm text-neutral-content/70">
                {t("tagline")}
              </p>
              <ul className="mt-4 flex gap-3 text-neutral-content/70">
                {socials.map(({ label, href, Icon }) => (
                  <li key={label}>
                    <a href={href} aria-label={label} className="hover:text-neutral-content">
                      <Icon className="size-4" aria-hidden />
                    </a>
                  </li>
                ))}
              </ul>
            </div>
            <div className="grid grid-cols-2 gap-8 text-sm sm:grid-cols-3 sm:gap-10">
              <div>
                <h2 className="font-semibold">{t("product")}</h2>
                <ul className="mt-3 space-y-2 text-neutral-content/70">
                  {sections.map((s) => (
                    <li key={s.href}><Link href={s.href} className="hover:text-neutral-content">{t(`nav.${s.key}`)}</Link></li>
                  ))}
                  <li><Link href="/docs" className="hover:text-neutral-content">{t("nav.docs")}</Link></li>
                </ul>
              </div>
              <div>
                <h2 className="font-semibold">{t("account")}</h2>
                <ul className="mt-3 space-y-2 text-neutral-content/70">
                  <li><Link href="/login" className="hover:text-neutral-content">{t("logIn")}</Link></li>
                  <li><Link href="/signup" className="hover:text-neutral-content">{t("signUp")}</Link></li>
                </ul>
              </div>
              <div>
                <h2 className="font-semibold">{t("contact")}</h2>
                <a href="mailto:info@stockwise.com" className="mt-3 block text-neutral-content/70 hover:text-neutral-content">info@stockwise.com</a>
              </div>
            </div>
          </div>

          <div className="mt-14 flex flex-col justify-between gap-4 border-t border-neutral-content/15 py-6 text-xs text-neutral-content/70 sm:flex-row">
            <p>{t("copyright")}</p>
            <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
              <LanguageSelect scope="browser" compact />
              <Link href="/privacy" className="underline hover:text-neutral-content">{t("privacy")}</Link>
              {/* Atribusi wajib lisensi gratis Storyset (ilustrasi di halaman For You). */}
              <a href="https://storyset.com" target="_blank" rel="noopener noreferrer" className="underline hover:text-neutral-content">
                {t("illustrationsBy")}
              </a>
              <a href="#top" className="underline hover:text-neutral-content">{t("backToTop")}</a>
            </div>
          </div>
        </div>
        <p aria-hidden className="pointer-events-none -mb-[0.22em] select-none text-center text-[18vw] font-bold leading-none tracking-tight text-neutral-content/5">
          Stockwise
        </p>
      </footer>
    </>
  );
}
