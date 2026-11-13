import { apiDelete, apiGet, apiPut } from "@/lib/api-client";
import type { GeoPlace, Outlet, SetLocationRequest, UpdateOutletRequest } from "../types";

export const updateOutlet = (input: UpdateOutletRequest) => apiPut<Outlet>("/outlet", input);

/** Menghapus usaha beserta semua datanya secara permanen; `confirmName` harus sama persis dengan nama usaha. */
export const deleteOutlet = (confirmName: string) => apiDelete<null>("/outlet", { confirmName });

export const setLocation = (input: SetLocationRequest) => apiPut<Outlet>("/outlet/location", input);
export const clearLocation = () => apiDelete<Outlet>("/outlet/location");

export function searchPlaces(q: string, near?: { latitude: number; longitude: number }) {
  const params = new URLSearchParams({ q });
  if (near) {
    params.set("latitude", String(near.latitude));
    params.set("longitude", String(near.longitude));
  }
  return apiGet<GeoPlace[]>(`/geo/search?${params}`);
}
