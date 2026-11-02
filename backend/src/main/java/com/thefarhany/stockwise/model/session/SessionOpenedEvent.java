package com.thefarhany.stockwise.model.session;

import com.thefarhany.stockwise.model.session.DailySession;

/** Diterbitkan di dalam transaksi buka sesi, setelah baris stok awal dibuat. */
public record SessionOpenedEvent(DailySession session) {
}
