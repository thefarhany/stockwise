import { apiPost } from "@/lib/api-client";
import type { InsightResult } from "@/features/reports/types";

/** Membuat (ulang) insight sesi tertutup; maksimal 3 kali per sesi. */
export const generateInsight = (sessionId: string) =>
  apiPost<InsightResult>(`/sessions/${encodeURIComponent(sessionId)}/insight`);
