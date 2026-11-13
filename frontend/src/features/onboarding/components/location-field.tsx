"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { type FieldErrors, type UseFormRegister, type UseFormSetValue } from "react-hook-form";
import { LuMapPin, LuSearch } from "react-icons/lu";
import { Field } from "@/components/ui/field";
import { ApiError, errorMessage } from "@/lib/api-client";
import { clearLocation, searchPlaces, setLocation } from "../api/outlet";
import type { ProfileInput } from "../schemas";
import type { Address, GeoPlace, Outlet, SetLocationRequest } from "../types";

const parts = ["street", "village", "district", "city", "province", "postalCode"] as const;

/** Alamat untuk form: null jadi teks kosong. */
export const addressInput = (a: Address | null): ProfileInput["address"] => ({
  full: a?.full ?? "",
  street: a?.street ?? "",
  village: a?.village ?? "",
  district: a?.district ?? "",
  city: a?.city ?? "",
  province: a?.province ?? "",
  postalCode: a?.postalCode ?? "",
});

/**
 * Lokasi usaha. "Use my current location" dan memilih hasil pencarian langsung menyimpan lokasi (alamat dicari backend
 * lewat Mapbox); mengubah bagian alamat secara manual ikut tersimpan lewat tombol Save form.
 */
export function LocationField({
  register,
  setValue,
  errors,
  latitude,
  longitude,
  full,
}: {
  register: UseFormRegister<ProfileInput>;
  setValue: UseFormSetValue<ProfileInput>;
  errors: FieldErrors<ProfileInput>;
  latitude: number | null;
  longitude: number | null;
  full: string;
}) {
  const router = useRouter();
  const t = useTranslations("location");
  const [busy, setBusy] = useState<"locate" | "save" | "remove" | null>(null);
  const [message, setMessage] = useState<{ text: string; error: boolean }>();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<GeoPlace[] | null>(null);
  const [searching, setSearching] = useState(false);
  const hasLocation = latitude !== null || full.trim() !== "";

  // Cari setelah berhenti mengetik 400 ms, minimal 3 huruf (sama dengan backend).
  useEffect(() => {
    const q = query.trim();
    if (q.length < 3) return;
    let stale = false;
    const timer = setTimeout(() => {
      setSearching(true);
      searchPlaces(q, latitude !== null && longitude !== null ? { latitude, longitude } : undefined)
        .then((r) => !stale && setResults(r))
        .catch((e) => !stale && fail(e))
        .finally(() => !stale && setSearching(false));
    }, 400);
    return () => {
      stale = true;
      clearTimeout(timer);
    };
    // fail hanya memakai setter; koordinat dibaca saat pencarian dimulai.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  function apply(outlet: Outlet, text: string) {
    setValue("latitude", outlet.latitude);
    setValue("longitude", outlet.longitude);
    setValue("address", addressInput(outlet.address));
    setMessage({ text, error: false });
  }

  function fail(e: unknown) {
    if (e instanceof ApiError && e.status === 401) return router.replace("/login");
    setMessage({ text: e instanceof ApiError ? e.message : errorMessage(""), error: true });
  }

  async function save(kind: "locate" | "save" | "remove", run: () => Promise<Outlet>, text: string) {
    setBusy(kind);
    setMessage(undefined);
    try {
      apply(await run(), text);
    } catch (e) {
      fail(e);
    } finally {
      setBusy(null);
    }
  }

  function locate() {
    if (!navigator.geolocation) {
      setMessage({ text: t("noGeolocation"), error: true });
      return;
    }
    setBusy("locate");
    setMessage(undefined);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) =>
        save("locate", () => setLocation({ latitude: coords.latitude, longitude: coords.longitude }), t("located")),
      () => {
        setBusy(null);
        setMessage({ text: t("blocked"), error: true });
      },
      { timeout: 10_000, enableHighAccuracy: true },
    );
  }

  function pick(place: GeoPlace) {
    setQuery("");
    setResults(null);
    if (place.latitude === null || place.longitude === null) return;
    const input: SetLocationRequest = { latitude: place.latitude, longitude: place.longitude, address: place.address };
    save("save", () => setLocation(input), t("addressSaved"));
  }

  return (
    <div role="group" aria-labelledby="location-label">
      <p id="location-label" className="text-sm font-semibold">
        {t("label")} <span className="ml-1 text-xs font-normal text-base-content/60">{t("optional")}</span>
      </p>

      <div className="mt-1.5 flex flex-wrap items-center justify-between gap-3 rounded-field border border-base-300 bg-white/70 px-3.5 py-2.5 text-sm">
        <span className="flex min-w-0 items-start gap-2.5">
          <LuMapPin aria-hidden className="mt-0.5 size-4 shrink-0 text-base-content/60" />
          {hasLocation ? (
            <span className="min-w-0">
              <span className="block">{full.trim() || t("notFoundForPoint")}</span>
              {latitude !== null && longitude !== null && (
                <span className="block text-xs text-base-content/60">
                  {latitude.toFixed(6)}, {longitude.toFixed(6)}
                </span>
              )}
            </span>
          ) : (
            <span className="text-base-content/50">{t("noLocation")}</span>
          )}
        </span>
        <span className="flex gap-1">
          <button type="button" onClick={locate} disabled={busy !== null} className="btn btn-ghost btn-sm">
            {busy === "locate" && <span aria-hidden className="loading loading-spinner loading-xs" />}
            {busy === "locate" ? t("finding") : t("useCurrent")}
          </button>
          {hasLocation && (
            <button type="button" onClick={() => save("remove", clearLocation, t("removed"))} disabled={busy !== null} className="btn btn-ghost btn-sm">
              {t("remove")}
            </button>
          )}
        </span>
      </div>

      <div className="relative mt-2" onBlur={(e) => !e.currentTarget.contains(e.relatedTarget) && setResults(null)}>
        <label className="input w-full border-base-300 bg-white/70">
          <LuSearch aria-hidden className="size-4 text-base-content/50" />
          <span className="sr-only">{t("search")}</span>
          <input
            type="search"
            autoComplete="off"
            placeholder={t("searchPlaceholder")}
            value={query}
            disabled={busy !== null}
            onChange={(e) => {
              setQuery(e.target.value);
              if (e.target.value.trim().length < 3) setResults(null);
            }}
            onKeyDown={(e) => e.key === "Escape" && setResults(null)}
          />
          {(searching || busy === "save") && <span aria-hidden className="loading loading-spinner loading-xs" />}
        </label>
        {results !== null && (
          <ul aria-label={t("found")} className="absolute z-20 mt-1 w-full overflow-hidden rounded-box border border-base-300 bg-white p-1 shadow-lg">
            {results.map((r, i) => (
              <li key={i}>
                <button type="button" className="block w-full rounded-field px-3 py-2 text-left text-sm hover:bg-base-200 focus-visible:bg-base-200 focus-visible:outline-none" onClick={() => pick(r)}>
                  {r.address.full}
                </button>
              </li>
            ))}
            {results.length === 0 && <li className="px-3 py-2 text-sm text-base-content/60">{t("noneFound")}</li>}
          </ul>
        )}
      </div>

      <p className={`mt-1 text-xs ${message?.error ? "text-error" : "text-base-content/60"}`} aria-live="polite">
        {message?.text ?? t("hint")}
      </p>

      <details className="mt-3 rounded-box border border-base-300 bg-white/50 px-5 py-4" open={errors.address ? true : undefined}>
        <summary className="cursor-pointer text-sm font-semibold">{t("editDetails")}</summary>
        <p className="mt-1 text-xs text-base-content/60">{t("editDetailsHint")}</p>
        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Field id="address-full" label={t("fullAddress")} autoComplete="off" error={errors.address?.full?.message} {...register("address.full")} />
          </div>
          {parts.map((p) => (
            <Field key={p} id={`address-${p}`} label={t(`parts.${p}`)} autoComplete="off" error={errors.address?.[p]?.message} {...register(`address.${p}`)} />
          ))}
        </div>
      </details>
    </div>
  );
}
