// Kontrak modul auth dan akun staf (backend dto/auth). Isi = field `data` di amplop.
import type { Id, LocalTime } from "@/lib/api-types";
import type { Currency } from "@/lib/currency";
import type { Language } from "@/i18n/locale";

export type Role = "OWNER" | "CASHIER";

export type User = {
  id: Id;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  /** Bahasa tampilan akun ini. */
  language: Language;
};

/** Ringkasan usaha di respons auth dan /me. */
export type OutletInfo = {
  id: Id;
  name: string;
  timezone: string;
  currency: Currency;
  openTime: LocalTime | null;
  closeTime: LocalTime | null;
  setupCompleted: boolean;
};

/** POST /auth/register (body). Kata sandi 8–72 karakter. */
export type RegisterRequest = {
  name: string;
  email: string;
  password: string;
  outletName: string;
  /** Bahasa awal akun Owner: bahasa yang sedang dipakai di halaman daftar. */
  language?: Language;
};

/** POST /auth/login (body). */
export type LoginRequest = { email: string; password: string };

/** POST /auth/register (201) dan POST /auth/login (200). */
export type AuthResponse = {
  user: User;
  outlet: OutletInfo;
  accessToken: string;
  refreshToken: string;
  /** Detik sampai access token kedaluwarsa (900). */
  expiresIn: number;
};

/** POST /auth/refresh dan POST /auth/logout (body). */
export type RefreshRequest = { refreshToken: string };

/** POST /auth/refresh. Refresh token dirotasi. POST /auth/logout menjawab data: null. */
export type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
};

/** GET /me 🧾 */
export type Profile = { user: User; outlet: OutletInfo };

// --- Akun staf (OWNER) ---
// GET /users → User[] (tanpa halaman)

/** POST /users (body) → 201 User. Akun yang dibuat selalu CASHIER. */
export type CreateUserRequest = { name: string; email: string; password: string };

/** PUT /users/{id} (body) → User. Field kosong = tidak diubah. */
export type UpdateUserRequest = { name?: string; active?: boolean };
