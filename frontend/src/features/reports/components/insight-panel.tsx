"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { generateInsight } from "@/features/reports/api/insight";
import type { InsightResult } from "@/features/reports/types";
import { ApiError, errorMessage } from "@/lib/api-client";
import { formatClock } from "@/lib/format";

/** Insight AI sesi tertutup: buat atau buat ulang, maksimal 3 kali per sesi. */
export function InsightPanel({ sessionId, initial, timeZone }: { sessionId: string; initial: InsightResult; timeZone: string }) {
  const router = useRouter();
  const t = useTranslations("insight");
  const [insight, setInsight] = useState(initial);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const ready = insight.status === "READY" && insight.text;

  async function generate() {
    setPending(true);
    setError(null);
    try {
      setInsight(await generateInsight(sessionId));
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) return router.replace("/login");
      if (!(e instanceof ApiError)) console.error(e);
      setError(e instanceof ApiError ? e.message : errorMessage(""));
    } finally {
      setPending(false);
    }
  }

  const left = insight.attemptsLeft;
  return (
    <div className="bg-base-200 px-6 py-5" aria-live="polite">
      <h3 className="text-xs uppercase tracking-widest text-base-content/70">
        {ready && insight.generatedAt ? t("madeAt", { time: formatClock(insight.generatedAt, timeZone) }) : t("title")}
      </h3>
      <p className="mt-2">
        {ready ? insight.text : t("prompt")}
      </p>
      {error && <p className="mt-2 text-sm text-error" role="alert">{error}</p>}
      {left > 0 && (
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <button type="button" className={`btn btn-sm ${ready ? "border-base-300 bg-white" : "btn-primary"}`} disabled={pending} onClick={generate}>
            {pending && <span aria-hidden className="loading loading-spinner loading-xs" />}
            {pending ? t("making") : ready ? t("regenerate") : t("make")}
          </button>
          <span className="text-xs text-base-content/60">
            {ready ? t("triesLeft", { count: left }) : t("triesForSession", { count: left })}
          </span>
        </div>
      )}
    </div>
  );
}
