package com.thefarhany.stockwise.controller.auth;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.auth.AuthResponse;
import com.thefarhany.stockwise.dto.auth.LoginRequest;
import com.thefarhany.stockwise.dto.auth.RefreshRequest;
import com.thefarhany.stockwise.dto.auth.RegisterRequest;
import com.thefarhany.stockwise.dto.auth.TokenResponse;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	private final AuthService authService;

	AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.ok(authService.register(request));
	}

	@PostMapping("/login")
	ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	@PostMapping("/refresh")
	ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ApiResponse.ok(authService.refresh(request.refreshToken()));
	}

	@PostMapping("/logout")
	ApiResponse<Void> logout(@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody RefreshRequest request) {
		authService.logout(principal.userId(), request.refreshToken());
		return ApiResponse.ok(null);
	}

}
