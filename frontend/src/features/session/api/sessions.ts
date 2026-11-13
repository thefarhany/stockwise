import { apiPost } from "@/lib/api-client";
import type { CloseSessionRequest, OpenSessionRequest, Session } from "@/features/session/types";

export const openSession = (input: OpenSessionRequest) => apiPost<Session>("/sessions/open", input);

export const closeSession = (sessionId: string, input: CloseSessionRequest) =>
  apiPost<Session>(`/sessions/${encodeURIComponent(sessionId)}/close`, input);
