package com.thefarhany.stockwise.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

class RateLimitTest {

	static class MutableClock extends Clock {

		Instant now = Instant.parse("2026-11-01T00:00:00Z");

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

	@RestController
	static class Probe {

		@PostMapping("/api/v1/auth/login")
		void login() {
		}

		@PostMapping("/api/v1/auth/refresh")
		void refresh() {
		}

		@GetMapping("/api/v1/auth/login")
		void get() {
		}

	}

	@Test
	void blocksAfterMaxThenResetsWhenWindowPasses() {
		MutableClock clock = new MutableClock();
		RateLimiter limiter = new RateLimiter(clock, 3, 60);
		assertThat(limiter.tryAcquire("a")).isZero();
		assertThat(limiter.tryAcquire("a")).isZero();
		assertThat(limiter.tryAcquire("a")).isZero();
		assertThat(limiter.tryAcquire("a")).isEqualTo(60);
		assertThat(limiter.tryAcquire("b")).as("kunci lain tidak terpengaruh").isZero();

		clock.now = clock.now.plus(Duration.ofSeconds(45));
		assertThat(limiter.tryAcquire("a")).as("sisa tunggu turun").isEqualTo(15);

		clock.now = clock.now.plus(Duration.ofSeconds(15));
		assertThat(limiter.tryAcquire("a")).isZero();
	}

	@Test
	void filterReturnsTooManyRequestsOnlyForLoginAndRegisterPost() throws Exception {
		RateLimiter limiter = new RateLimiter(new MutableClock(), 2, 60);
		MockMvc mvc = MockMvcBuilders.standaloneSetup(new Probe())
				.addFilters(new AuthRateLimitFilter(limiter, JsonMapper.builder().build())).build();

		mvc.perform(post("/api/v1/auth/login")).andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/login")).andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/login"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "60"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("TOO_MANY_REQUESTS"))
				.andExpect(jsonPath("$.details.retryAfterSeconds").value(60));

		// refresh dan GET tidak dibatasi
		mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/login")).andExpect(status().isOk());
	}

}
