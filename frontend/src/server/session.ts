// Khusus BFF: token Core API hanya hidup di cookie httpOnly, tidak pernah terlihat oleh JavaScript browser.
import type { TokenResponse } from "@/features/auth/types";

export const CORE_API_URL = process.env.CORE_API_URL ?? "http://localhost:8080/api/v1";

export const ACCESS_COOKIE = "sw_access";
// Nama baru sejak cookie ini berpath "/": cookie lama "sw_refresh" (path /api) masih terkirim ke /api dan akan bentrok.
export const REFRESH_COOKIE = "sw_refresh_token";
const LEGACY_REFRESH_COOKIE = "sw_refresh";
const REFRESH_MAX_AGE = 30 * 24 * 60 * 60; // sama dengan umur refresh token backend (30 hari)

type CookieWriter = {
  set(name: string, value: string, options: Record<string, unknown>): unknown;
  delete(name: string): unknown;
};

const base = { httpOnly: true, sameSite: "strict", secure: process.env.NODE_ENV === "production" } as const;

export function writeSession(cookies: CookieWriter, tokens: TokenResponse) {
  cookies.set(ACCESS_COOKIE, tokens.accessToken, { ...base, path: "/", maxAge: tokens.expiresIn });
  // Path "/" supaya proxy.ts bisa memperbarui sesi sebelum halaman dirender; tetap httpOnly + SameSite=Strict.
  cookies.set(REFRESH_COOKIE, tokens.refreshToken, { ...base, path: "/", maxAge: REFRESH_MAX_AGE });
  cookies.set(LEGACY_REFRESH_COOKIE, "", { ...base, path: "/api", maxAge: 0 });
}

export function clearSession(cookies: CookieWriter) {
  cookies.set(ACCESS_COOKIE, "", { ...base, path: "/", maxAge: 0 });
  cookies.set(REFRESH_COOKIE, "", { ...base, path: "/", maxAge: 0 });
  cookies.set(LEGACY_REFRESH_COOKIE, "", { ...base, path: "/api", maxAge: 0 });
}

/** true bila access token (JWT) sudah habis atau habis dalam `withinSeconds` detik. Token rusak dianggap habis. */
export function expiresSoon(accessToken: string, withinSeconds = 60, now = Date.now()): boolean {
  try {
    const payload = JSON.parse(Buffer.from(accessToken.split(".")[1] ?? "", "base64url").toString("utf8")) as { exp?: number };
    return typeof payload.exp !== "number" || payload.exp * 1000 - now < withinSeconds * 1000;
  } catch {
    return true;
  }
}

// Refresh token sekali pakai (dirotasi backend). Permintaan paralel dengan token yang sama (mis. prefetch halaman)
// harus berbagi satu hasil; hasilnya disimpan sebentar untuk permintaan yang datang sebelum cookie baru sampai.
// shortcut: hanya dalam satu proses server; pakai penyimpanan bersama (mis. Redis) bila frontend jalan di banyak instance.
const inflight = new Map<string, Promise<TokenResponse | null>>();
const REUSE_MS = 30_000;

/** Tukar refresh token dengan pasangan token baru; null bila ditolak backend atau backend tidak terjangkau. */
export function refreshTokens(refreshToken: string): Promise<TokenResponse | null> {
  const pending = inflight.get(refreshToken);
  if (pending) return pending;
  const result = (async () => {
    const res = await callCoreApi("/auth/refresh", { method: "POST", body: { refreshToken } });
    if (!res) return null;
    const json = (await res.json().catch(() => null)) as { success: boolean; data: TokenResponse } | null;
    return json?.success ? json.data : null;
  })();
  inflight.set(refreshToken, result);
  setTimeout(() => inflight.delete(refreshToken), REUSE_MS).unref?.();
  return result;
}

/** Perlindungan CSRF untuk permintaan yang mengubah data: Origin wajib sama dengan host yang melayani. */
export function isSameOrigin(request: Request): boolean {
  const origin = request.headers.get("origin");
  const host = request.headers.get("x-forwarded-host") ?? request.headers.get("host");
  if (!origin || !host) return false;
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}

/** Amplop error dengan bentuk yang sama seperti Core API. */
export function errorResponse(status: number, errorCode: string, message: string) {
  return Response.json(
    { success: false, message, errorCode, data: null, timestamp: new Date().toISOString() },
    { status },
  );
}

/** Teruskan JSON ke Core API. Gagal terhubung menjadi null, bukan exception. */
export async function callCoreApi(
  path: string,
  init: { method: string; body?: unknown; accessToken?: string; forwardedFor?: string | null },
) {
  try {
    return await fetch(`${CORE_API_URL}${path}`, {
      method: init.method,
      headers: {
        "Content-Type": "application/json",
        ...(init.accessToken ? { Authorization: `Bearer ${init.accessToken}` } : {}),
        ...(init.forwardedFor ? { "X-Forwarded-For": init.forwardedFor } : {}),
      },
      body: init.body === undefined ? undefined : JSON.stringify(init.body),
      cache: "no-store",
    });
  } catch {
    return null;
  }
}
