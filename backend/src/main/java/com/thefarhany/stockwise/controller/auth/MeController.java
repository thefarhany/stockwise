package com.thefarhany.stockwise.controller.auth;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.auth.LanguageRequest;
import com.thefarhany.stockwise.dto.auth.ProfileResponse;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.auth.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	/** Bahasa tampilan akun sendiri; semua peran boleh. */
	@PutMapping("/api/v1/me/language")
	ApiResponse<ProfileResponse> language(@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody LanguageRequest r) {
		return ApiResponse.ok(authService.setLanguage(principal.userId(), r.language()));
	}

}
