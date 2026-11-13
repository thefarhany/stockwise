import { cache } from "react";
import type { Profile } from "@/features/auth/types";
import { coreGet } from "./core-get";

/** GET /me sekali per request, dipakai bersama oleh halaman dan penentuan bahasa (i18n/request.ts). */
export const getProfile = cache(() => coreGet<Profile>("/me"));
