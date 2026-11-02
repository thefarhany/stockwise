package com.thefarhany.stockwise.utils.outlet;

import static org.assertj.core.api.Assertions.assertThat;

import com.thefarhany.stockwise.model.outlet.Outlet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OutletRulesTest {

	@Test
	void businessDateFollowsOutletZoneNotUtc() {
		// 18:00 UTC tanggal 31 = 01:00 WIB tanggal 1
		Clock clock = Clock.fixed(Instant.parse("2026-10-31T18:00:00Z"), ZoneOffset.UTC);
		Outlet outlet = new Outlet("U");
		assertThat(OutletRules.zone(outlet)).isEqualTo(ZoneId.of("Asia/Jakarta"));
		assertThat(OutletRules.today(outlet, clock)).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026, 10, 31));
	}

	@Test
	void setupIsCompleteOnlyWhenBothHoursAreSet() {
		Outlet outlet = new Outlet("U");
		assertThat(OutletRules.isSetupCompleted(outlet)).isFalse();
		outlet.setOpenTime(LocalTime.of(11, 0));
		assertThat(OutletRules.isSetupCompleted(outlet)).isFalse();
		outlet.setCloseTime(LocalTime.of(21, 0));
		assertThat(OutletRules.isSetupCompleted(outlet)).isTrue();
	}

}
