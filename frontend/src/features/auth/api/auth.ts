import type { Language } from "@/i18n/locale";
import { apiPost, apiPut } from "@/lib/api-client";
import type { OutletInfo, Profile, User } from "../types";
import type { LoginInput, SignupInput } from "../schemas";

/** Respons BFF untuk login/daftar: token disimpan BFF di cookie, browser hanya menerima profil. */
export type SessionInfo = { user: User; outlet: OutletInfo };

export const login = (input: LoginInput) => apiPost<SessionInfo>("/auth/login", input);
export const signup = (input: SignupInput & { language: Language }) => apiPost<SessionInfo>("/auth/register", input);

/** Bahasa tampilan akun yang sedang login (semua peran). */
export const setLanguage = (language: Language) => apiPut<Profile>("/me/language", { language });
export const logout = () => apiPost<null>("/auth/logout");
