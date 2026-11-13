// Satu alur untuk POST /api/auth/login dan /api/auth/register: teruskan ke Core API, simpan token di cookie,
// kembalikan user + outlet saja (token tidak pernah dikirim ke browser).
import { cookies } from "next/headers";
import type { AuthResponse } from "@/features/auth/types";
import type { ApiResponse } from "@/lib/api-types";
import { callCoreApi, errorResponse, isSameOrigin, writeSession } from "./session";

export async function forwardAuth(request: Request, corePath: "/auth/login" | "/auth/register") {
  if (!isSameOrigin(request)) return errorResponse(403, "FORBIDDEN", "Request origin not allowed.");

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return errorResponse(400, "VALIDATION_ERROR", "Invalid request body.");
  }

  // Core API membatasi login/daftar per IP. Tanpa header ini semua pengguna terlihat dari satu IP (server ini) dan
  // berbagi satu batas. Rantai diteruskan apa adanya; Core API hanya memercayainya dari alamat jaringan privat.
  const forwardedFor = request.headers.get("x-forwarded-for");
  const res = await callCoreApi(corePath, { method: "POST", body, forwardedFor });
  if (!res) return errorResponse(502, "INTERNAL_ERROR", "Server is unreachable.");

  const json = (await res.json()) as ApiResponse<AuthResponse>;
  if (!json.success) return Response.json(json, { status: res.status });

  const { user, outlet, accessToken, refreshToken, expiresIn } = json.data;
  writeSession(await cookies(), { accessToken, refreshToken, expiresIn });
  return Response.json({ success: true, message: json.message, data: { user, outlet } }, { status: res.status });
}
