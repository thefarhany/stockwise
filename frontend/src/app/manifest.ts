import type { MetadataRoute } from "next";

// Bisa dipasang di layar utama HP (PWA). Belum login: /dashboard mengarahkan ke /login.
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Stockwise",
    short_name: "Stockwise",
    description: "Kasir, stok bahan, dan laporan harian untuk usaha kuliner.",
    lang: "id",
    start_url: "/dashboard",
    scope: "/",
    display: "standalone",
    background_color: "#f4f4f0",
    theme_color: "#f4f4f0",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
      { src: "/icons/maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
