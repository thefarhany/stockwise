// Pintu tunggal browser ke BFF (/api/...): membuka amplop ApiResponse dan melempar ApiError seragam.
import type { ApiErrorDetails, ApiResponse, ErrorCode } from "./api-types";
import { errorMessages } from "./error-messages";

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly errorCode: ErrorCode | "NETWORK_ERROR",
    readonly details?: ApiErrorDetails,
  ) {
    super(errorMessage(errorCode));
  }
}

// Pesan backend tidak dipakai: pesan ditentukan dari errorCode (kontrak API), dalam bahasa halaman (<html lang>).
export function errorMessage(code: string) {
  const m = errorMessages[typeof document !== "undefined" && document.documentElement.lang === "id" ? "id" : "en"];
  return m[code] ?? m.generic!;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers: { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "NETWORK_ERROR");
  }
  let json: ApiResponse<T>;
  try {
    json = await res.json();
  } catch {
    throw new ApiError(res.status, "INTERNAL_ERROR");
  }
  if (!json.success) throw new ApiError(res.status, json.errorCode, json.details);
  return json.data;
}

export const apiGet = <T>(path: string) => request<T>("GET", path);
export const apiPost = <T>(path: string, body?: unknown) => request<T>("POST", path, body);
export const apiPut = <T>(path: string, body: unknown) => request<T>("PUT", path, body);
export const apiDelete = <T>(path: string, body?: unknown) => request<T>("DELETE", path, body);
