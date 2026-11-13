import { cookies } from "next/headers";
import { callCoreApi, clearSession, errorResponse, isSameOrigin, REFRESH_COOKIE } from "@/server/session";

export async function POST(request: Request) {
  if (!isSameOrigin(request)) return errorResponse(403, "FORBIDDEN", "Request origin not allowed.");
  const store = await cookies();
  const refreshToken = store.get(REFRESH_COOKIE)?.value;
  // Cabut refresh token di backend bila ada; cookie tetap dihapus walau backend tidak terjangkau.
  if (refreshToken) await callCoreApi("/auth/logout", { method: "POST", body: { refreshToken } });
  clearSession(store);
  return Response.json({ success: true, message: "OK", data: null });
}
