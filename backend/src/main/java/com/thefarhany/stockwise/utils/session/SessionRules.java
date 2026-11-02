package com.thefarhany.stockwise.utils.session;

import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/** Aturan waktu sesi harian. */
public final class SessionRules {

	/** Setelah jam tutup usaha + selang ini, sesi ditutup otomatis. */
	public static final Duration AUTO_CLOSE_GRACE = Duration.ofMinutes(30);

	private SessionRules() {
	}

	/** Jam tutup terencana + 30 menit pada tanggal dagang, menurut zona waktu usaha. */
	public static Instant autoCloseAt(DailySession session) {
		return LocalDateTime.of(session.getBusinessDate(), session.getPlannedCloseTime()).plus(AUTO_CLOSE_GRACE)
				.atZone(OutletRules.zone(session.getOutlet())).toInstant();
	}

}
