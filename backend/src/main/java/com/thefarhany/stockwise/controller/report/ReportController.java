package com.thefarhany.stockwise.controller.report;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.report.DailyReportResponse;
import com.thefarhany.stockwise.dto.report.RangeReportResponse;
import com.thefarhany.stockwise.dto.report.SessionListItem;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.report.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Hanya Owner. Riwayat sesi memakai path /sessions (sumber dayanya sesi) tetapi hitungannya milik modul report. */
@RestController
@PreAuthorize("hasRole('OWNER')")
class ReportController {

	private final ReportService service;

	ReportController(ReportService service) {
		this.service = service;
	}

	@GetMapping("/api/v1/reports/daily")
	ApiResponse<DailyReportResponse> daily(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam LocalDate date) {
		return ApiResponse.ok(service.daily(me, date));
	}

	/** Tren omzet/HPP/laba per hari atau minggu; rentang wajib, paling panjang 366 hari. */
	@GetMapping("/api/v1/reports/range")
	ApiResponse<RangeReportResponse> range(@AuthenticationPrincipal AuthPrincipal me, @RequestParam LocalDate from,
			@RequestParam LocalDate to, @RequestParam(defaultValue = "day") String groupBy) {
		return ApiResponse.ok(service.range(me, from, to, groupBy));
	}

	@GetMapping("/api/v1/sessions")
	ApiResponse<List<SessionListItem>> history(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "31") int size) {
		return ApiResponse.page(service.history(me, from, to, page, size));
	}

}
