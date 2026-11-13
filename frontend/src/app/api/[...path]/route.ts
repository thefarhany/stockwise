// Proxy BFF untuk semua endpoint Core API selain auth: tempel access token dari cookie, dan bila token habis,
// perbarui sekali lewat refresh token lalu ulangi permintaan.
import { cookies } from "next/headers";
import {
  ACCESS_COOKIE,
  callCoreApi,
  clearSession,
  errorResponse,
  isSameOrigin,
  REFRESH_COOKIE,
  refreshTokens,
  writeSession,
} from "@/server/session";

type CookieStore = Awaited<ReturnType<typeof cookies>>;

async function refreshSession(store: CookieStore): Promise<string | null> {
  const refreshToken = store.get(REFRESH_COOKIE)?.value;
  if (!refreshToken) return null;
  const tokens = await refreshTokens(refreshToken);
  if (!tokens) {
    clearSession(store);
    return null;
  }
  writeSession(store, tokens);
  return tokens.accessToken;
}

async function handle(request: Request, { params }: RouteContext<"/api/[...path]">) {
  const { path } = await params;
  // Auth punya route sendiri karena token tidak boleh sampai ke browser.
  if (path[0] === "auth") return errorResponse(404, "NOT_FOUND", "Not found.");
  if (request.method !== "GET" && !isSameOrigin(request)) {
    return errorResponse(403, "FORBIDDEN", "Request origin not allowed.");
  }

  let body: unknown;
  if (request.method !== "GET") {
    const text = await request.text();
    try {
      body = text ? JSON.parse(text) : undefined;
    } catch {
      return errorResponse(400, "VALIDATION_ERROR", "Invalid request body.");
    }
  }

  const corePath = `/${path.map(encodeURIComponent).join("/")}${new URL(request.url).search}`;
  const call = (accessToken: string) => callCoreApi(corePath, { method: request.method, body, accessToken });

  const store = await cookies();
  const accessToken = store.get(ACCESS_COOKIE)?.value;
  let res = accessToken ? await call(accessToken) : undefined;
  if (res === undefined || res?.status === 401) {
    const fresh = await refreshSession(store);
    if (fresh) res = await call(fresh);
    else if (res === undefined) return errorResponse(401, "UNAUTHORIZED", "Please log in again.");
  }
  if (!res) return errorResponse(502, "INTERNAL_ERROR", "Server is unreachable.");

  const headers = new Headers({ "Content-Type": res.headers.get("content-type") ?? "application/json" });
  // Ekspor Excel: nama berkas unduhan dikirim backend lewat Content-Disposition.
  const disposition = res.headers.get("content-disposition");
  if (disposition) headers.set("Content-Disposition", disposition);
  return new Response(res.body, { status: res.status, headers });
}

export { handle as GET, handle as POST, handle as PUT, handle as DELETE };
