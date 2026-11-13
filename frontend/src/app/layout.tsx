import type { Metadata, Viewport } from "next";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getTranslations } from "next-intl/server";
import { Geist, Geist_Mono } from "next/font/google";
import { ServiceWorker } from "@/components/service-worker";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export async function generateMetadata(): Promise<Metadata> {
  return {
    title: "Stockwise",
    description: (await getTranslations("meta"))("description"),
    appleWebApp: { capable: true, title: "Stockwise", statusBarStyle: "default" },
  };
}

export const viewport: Viewport = { themeColor: "#f4f4f0" };

export default async function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang={await getLocale()}
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col font-sans">
        <NextIntlClientProvider>{children}</NextIntlClientProvider>
        <ServiceWorker />
      </body>
    </html>
  );
}
