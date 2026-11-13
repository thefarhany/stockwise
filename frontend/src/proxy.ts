// Perbarui sesi sebelum halaman dirender: server component hanya bisa membaca cookie, tidak bisa menulisnya.
import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE, clearSession, expiresSoon, REFRESH_COOKIE, refreshTokens, writeSession } from "@/server/session";

/** Path halaman untuk i18n/request.ts: halaman publik memakai bahasa browser, aplikasi memakai bahasa akun. */
const withPath = (request: NextRequest) => {
  const headers = new Headers(request.headers);
  headers.set("x-sw-path", request.nextUrl.pathname);
  return { request: { headers } };
};

export async function proxy(request: NextRequest) {
  const access = request.cookies.get(ACCESS_COOKIE)?.value;
  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
  if (!refresh || (access && !expiresSoon(access))) return NextResponse.next(withPath(request));

  const tokens = await refreshTokens(refresh);
  if (!tokens) {
    // Refresh token ditolak (habis, dicabut, akun nonaktif): hapus cookie; halaman sendiri yang mengarahkan ke login.
    const response = NextResponse.next(withPath(request));
    clearSession(response.cookies);
    return response;
  }
  // Token baru dipakai halaman ini (header Cookie request) dan disimpan browser (Set-Cookie response).
  request.cookies.set(ACCESS_COOKIE, tokens.accessToken);
  request.cookies.set(REFRESH_COOKIE, tokens.refreshToken);
  const response = NextResponse.next(withPath(request));
  writeSession(response.cookies, tokens);
  return response;
}

export const config = {
  // Route /api memperbarui sesinya sendiri (app/api/[...path]); berkas statis tidak butuh sesi.
  matcher: ["/((?!api/|_next/static|_next/image|favicon.ico|.*\\.[\\w]+$).*)"],
};
