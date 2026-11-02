package com.thefarhany.stockwise.utils.outlet;

import com.thefarhany.stockwise.model.outlet.Outlet;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Aturan waktu dan kelengkapan usaha. */
public final class OutletRules {

	private OutletRules() {
	}

	public static ZoneId zone(Outlet outlet) {
		return ZoneId.of(outlet.getTimezone());
	}

	/** Tanggal dagang hari ini menurut zona waktu usaha (bukan UTC). */
	public static LocalDate today(Outlet outlet, Clock clock) {
		return LocalDate.now(clock.withZone(zone(outlet)));
	}

	/** Usaha lengkap bila jam buka dan tutup sudah diisi (docs/04 bagian 3.1). */
	public static boolean isSetupCompleted(Outlet outlet) {
		return outlet.getOpenTime() != null && outlet.getCloseTime() != null;
	}

}
