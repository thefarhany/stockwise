package com.thefarhany.stockwise.controller.insight;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.insight.RecommendationResponse;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.insight.RecommendationService;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
class RecommendationController {

	private final RecommendationService service;

	RecommendationController(RecommendationService service) {
		this.service = service;
	}

	/** Tanpa date = tanggal dagang hari ini. Cashier boleh melihat, tanpa angka biaya. */
	@GetMapping
	ApiResponse<RecommendationResponse> get(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) LocalDate date) {
		return ApiResponse.ok(service.get(me, date));
	}

}
