package com.thefarhany.stockwise.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Jam yang bisa digeser, HANYA untuk profil `simulation` (tools/simulator mengisi data berbulan-bulan lewat API).
 * Tidak pernah aktif di profil default atau prod; ada tes yang menjaganya. Tanpa digeser, mengikuti jam sistem.
 */
@Component
@Primary
@Profile("simulation")
public class SimulationClock extends Clock {

	private volatile Instant fixed;

	public void set(Instant instant) {
		this.fixed = instant;
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
		SimulationClock self = this;
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
