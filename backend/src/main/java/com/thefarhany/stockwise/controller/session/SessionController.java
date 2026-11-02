package com.thefarhany.stockwise.controller.session;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.session.AddStockRequest;
import com.thefarhany.stockwise.dto.session.CloseSessionRequest;
import com.thefarhany.stockwise.dto.session.OpenSessionRequest;
import com.thefarhany.stockwise.dto.session.SessionResponse;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.session.SessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
class SessionController {

	private final SessionService service;

	SessionController(SessionService service) {
		this.service = service;
	}

	@GetMapping("/current")
	ApiResponse<SessionResponse> current(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(service.current(me));
	}

	@PostMapping("/open")
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<SessionResponse> open(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody(required = false) OpenSessionRequest request) {
		return ApiResponse.ok(service.open(me, request));
	}

	@PostMapping("/{id}/stock")
	ApiResponse<SessionResponse> addStock(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id,
			@Valid @RequestBody AddStockRequest request) {
		return ApiResponse.ok(service.addStock(me, id, request));
	}

	@PostMapping("/{id}/close")
	ApiResponse<SessionResponse> close(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id,
			@Valid @RequestBody CloseSessionRequest request) {
		return ApiResponse.ok(service.close(me, id, request));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	ApiResponse<SessionResponse> detail(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id) {
		return ApiResponse.ok(service.detail(me, id));
	}

}
