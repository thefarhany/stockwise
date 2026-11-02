package com.thefarhany.stockwise.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pembatas jendela tetap per kunci, di memori.
 * shortcut: state per instance dan hilang saat restart; ganti ke penyimpanan bersama kalau API dijalankan lebih dari satu instance.
 */
@Component
public class RateLimiter {

	private record Window(Instant start, int count) {
	}

	private static final int SWEEP_THRESHOLD = 10_000;

	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
	private final Clock clock;
	private final int max;
	private final Duration window;

	public RateLimiter(Clock clock, @Value("${stockwise.rate-limit.auth-max}") int max,
			@Value("${stockwise.rate-limit.window-seconds}") long windowSeconds) {
		this.clock = clock;
		this.max = max;
		this.window = Duration.ofSeconds(windowSeconds);
	}

	/** 0 bila diizinkan; selain itu jumlah detik sampai jendela berikutnya. */
	public long tryAcquire(String key) {
		Instant now = Instant.now(clock);
		if (windows.size() > SWEEP_THRESHOLD) {
			windows.values().removeIf(w -> !now.isBefore(w.start().plus(window)));
		}
		long[] retryAfter = {0};
		windows.compute(key, (k, w) -> {
			if (w == null || !now.isBefore(w.start().plus(window))) {
				return new Window(now, 1);
			}
			if (w.count() >= max) {
				retryAfter[0] = Math.max(1, Duration.between(now, w.start().plus(window)).toSeconds());
				return w;
			}
			return new Window(w.start(), w.count() + 1);
		});
		return retryAfter[0];
	}

}
