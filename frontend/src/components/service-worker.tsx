"use client";

import { useEffect } from "react";

/** Daftarkan public/sw.js (halaman offline PWA). Hanya di production supaya tidak mengganggu hot reload. */
export function ServiceWorker() {
  useEffect(() => {
    if (process.env.NODE_ENV === "production" && "serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
    }
  }, []);
  return null;
}
