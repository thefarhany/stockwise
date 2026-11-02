package com.thefarhany.stockwise.controller.auth;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.auth.ProfileResponse;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.auth.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

	private final AuthService authService;

	MeController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/api/v1/me")
	ApiResponse<ProfileResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
		return ApiResponse.ok(authService.profile(principal.userId()));
	}

}
