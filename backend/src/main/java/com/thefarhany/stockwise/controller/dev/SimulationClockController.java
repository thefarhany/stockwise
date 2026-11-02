package com.thefarhany.stockwise.controller.dev;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.config.SimulationClock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Menggeser jam server untuk simulator. Hanya ada di profil `simulation`; tetap butuh login seperti endpoint lain. */
@RestController
@Profile("simulation")
@RequestMapping("/api/v1/dev/clock")
class SimulationClockController {

	record SetClockRequest(@NotNull Instant instant) {
	}

	private final SimulationClock clock;

	SimulationClockController(SimulationClock clock) {
		this.clock = clock;
	}

	@GetMapping
	ApiResponse<Instant> now() {
		return ApiResponse.ok(clock.instant());
	}

	@PutMapping
	ApiResponse<Instant> set(@Valid @RequestBody SetClockRequest request) {
		clock.set(request.instant());
		return ApiResponse.ok(clock.instant());
	}

	@DeleteMapping
	ApiResponse<Instant> reset() {
		clock.reset();
		return ApiResponse.ok(clock.instant());
	}

}
