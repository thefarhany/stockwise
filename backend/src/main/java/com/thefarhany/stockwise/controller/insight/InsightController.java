package com.thefarhany.stockwise.controller.insight;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.insight.InsightResultResponse;
import com.thefarhany.stockwise.dto.insight.SummaryResponse;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.insight.InsightService;
import com.thefarhany.stockwise.service.insight.SummaryService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions/{id}")
@PreAuthorize("hasRole('OWNER')")
class InsightController {

	private final SummaryService summaryService;
	private final InsightService insightService;

	InsightController(SummaryService summaryService, InsightService insightService) {
		this.summaryService = summaryService;
		this.insightService = insightService;
	}

	@GetMapping("/summary")
	ApiResponse<SummaryResponse> summary(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id) {
		return ApiResponse.ok(summaryService.get(me, id));
	}

	@PostMapping("/insight")
	ApiResponse<InsightResultResponse> insight(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id) {
		return ApiResponse.ok(insightService.generate(me, id));
	}

}
