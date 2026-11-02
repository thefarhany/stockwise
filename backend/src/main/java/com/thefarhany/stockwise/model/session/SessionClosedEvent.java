package com.thefarhany.stockwise.model.session;

import com.thefarhany.stockwise.model.session.DailySession;

/** Diterbitkan di dalam transaksi tutup sesi (manual maupun otomatis), setelah semua kolom biaya terisi. */
public record SessionClosedEvent(DailySession session) {
}
