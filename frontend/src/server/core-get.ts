// Untuk server component: GET ke Core API memakai access token dari cookie.
import { cookies } from "next/headers";
import type { ApiResponse } from "@/lib/api-types";
import { ACCESS_COOKIE, callCoreApi } from "./session";

// Access token yang habis sudah diperbarui proxy.ts sebelum halaman dirender, jadi di sini cukup dibaca.
/** null bila belum login atau Core API tidak terjangkau. */
export async function coreGet<T>(path: string): Promise<ApiResponse<T> | null> {
  const token = (await cookies()).get(ACCESS_COOKIE)?.value;
  if (!token) return null;
  const res = await callCoreApi(path, { method: "GET", accessToken: token });
  try {
    return res ? ((await res.json()) as ApiResponse<T>) : null;
  } catch {
    return null;
  }
}
