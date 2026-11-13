import { apiPost, apiPut } from "@/lib/api-client";
import type { CreateUserRequest, UpdateUserRequest, User } from "@/features/auth/types";

/** Akun baru selalu CASHIER. */
export const createCashier = (input: CreateUserRequest) => apiPost<User>("/users", input);

/** `active: false` langsung mencabut sesi login akun itu. */
export const updateUser = (id: string, input: UpdateUserRequest) => apiPut<User>(`/users/${encodeURIComponent(id)}`, input);

