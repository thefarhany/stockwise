// Kontrak modul outlet: profil usaha dan panduan bertahap (backend dto/outlet). Semua endpoint OWNER.
import type { Id, LocalTime } from "@/lib/api-types";
import type { Currency } from "@/lib/currency";

export type Onboarding = {
  profileDone: boolean;
  stockItems: number;
  products: number;
  /** Jumlah produk aktif yang punya resep. */
  recipes: number;
  /** true bila profil lengkap dan ada minimal satu bahan, produk, dan resep. */
  readyToOpen: boolean;
};

/** Alamat usaha (backend dto/outlet/AddressDto). village = kelurahan/desa, district = kecamatan, city = kota/kabupaten. */
export type Address = {
  full: string | null;
  street: string | null;
  village: string | null;
  district: string | null;
  city: string | null;
  province: string | null;
  postalCode: string | null;
};

/** GET /geo/search?q=&latitude=&longitude= (OWNER) → GeoPlace[] (maks. 5, Indonesia), dari Mapbox. */
export type GeoPlace = { address: Address; latitude: number | null; longitude: number | null };

/**
 * PUT /outlet/location (OWNER) → Outlet. Tanpa address: backend mencari alamat dari koordinat lewat Mapbox lalu
 * menyimpan; dengan address: disimpan apa adanya. 503 GEOCODING_UNAVAILABLE / 502 GEOCODING_FAILED bila Mapbox tidak bisa.
 * DELETE /outlet/location (OWNER) → Outlet: koordinat dan alamat dihapus.
 */
export type SetLocationRequest = { latitude: number; longitude: number; address?: Address };

/** GET /outlet dan PUT /outlet. */
export type Outlet = {
  id: Id;
  name: string;
  timezone: string;
  currency: Currency;
  /** true setelah lapak pertama dibuka: mata uang tidak bisa diganti lagi. */
  currencyLocked: boolean;
  openTime: LocalTime | null;
  closeTime: LocalTime | null;
  latitude: number | null;
  longitude: number | null;
  address: Address | null;
  paydayDays: number[];
  recommendationBufferPct: number;
  targetMarginPct: number;
  setupCompleted: boolean;
  onboarding: Onboarding;
};

/** PUT /outlet (body). closeTime harus sesudah openTime. Mengganti currency saat currencyLocked → 409 CURRENCY_LOCKED. */
export type UpdateOutletRequest = {
  name: string;
  /** Kosong = tidak diubah. */
  currency?: Currency;
  openTime: LocalTime;
  closeTime: LocalTime;
  latitude?: number | null;
  longitude?: number | null;
  /** Kosong (atau semua bagiannya kosong) = alamat dihapus. */
  address?: { [K in keyof Address]: string | null } | null;
  /** Tanggal 1–31, tanpa dobel. */
  paydayDays: number[];
  /** 0–100. */
  recommendationBufferPct: number;
  /** 0–90; kosong = tidak diubah. */
  targetMarginPct?: number | null;
};

/** DELETE /outlet (body) → data: null. Hapus usaha dan semua datanya permanen. */
export type DeleteOutletRequest = { confirmName: string };
