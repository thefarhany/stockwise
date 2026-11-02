package com.thefarhany.stockwise;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Jam aplikasi untuk tes: ikut jam sistem sampai dikunci dengan set(), lalu reset(). */
public class TestClock extends Clock {

	private volatile Instant fixed;

	public void set(String isoInstant) {
		this.fixed = Instant.parse(isoInstant);
	}

	public void reset() {
		this.fixed = null;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		TestClock self = this;
		return new Clock() {
			@Override
			public ZoneId getZone() {
				return zone;
			}

			@Override
			public Clock withZone(ZoneId other) {
				return self.withZone(other);
			}

			@Override
			public Instant instant() {
				return self.instant();
			}
		};
	}

	@Override
	public Instant instant() {
		Instant f = fixed;
		return f != null ? f : Instant.now();
	}

}
