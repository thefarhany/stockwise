package com.thefarhany.stockwise.controller.sales;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.sales.CreateSaleRequest;
import com.thefarhany.stockwise.dto.sales.RecapRequest;
import com.thefarhany.stockwise.dto.sales.SaleDetailResponse;
import com.thefarhany.stockwise.dto.sales.SaleResponse;
import com.thefarhany.stockwise.dto.sales.SaleSummaryResponse;
import com.thefarhany.stockwise.dto.sales.VoidSaleRequest;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.sales.SaleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
class SaleController {

	private final SaleService service;

	SaleController(SaleService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<SaleResponse> create(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody CreateSaleRequest request) {
		return ApiResponse.ok(service.create(me, request));
	}

	@PostMapping("/recap")
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<SaleResponse> recap(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody RecapRequest request) {
		return ApiResponse.ok(service.recap(me, request));
	}

	@GetMapping
	ApiResponse<List<SaleSummaryResponse>> history(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) UUID sessionId, @RequestParam(required = false) LocalDate date,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.page(service.history(me, sessionId, date, page, size));
	}

	@GetMapping("/{id}")
	ApiResponse<SaleDetailResponse> get(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id) {
		return ApiResponse.ok(service.get(me, id));
	}

	@PostMapping("/{id}/void")
	ApiResponse<SaleDetailResponse> voidSale(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id,
			@Valid @RequestBody VoidSaleRequest request) {
		return ApiResponse.ok(service.voidSale(me, id, request.reason()));
	}

}
