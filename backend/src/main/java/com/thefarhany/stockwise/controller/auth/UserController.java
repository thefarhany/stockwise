package com.thefarhany.stockwise.controller.auth;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.auth.CreateUserRequest;
import com.thefarhany.stockwise.dto.auth.UpdateUserRequest;
import com.thefarhany.stockwise.dto.auth.UserInfo;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.auth.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('OWNER')")
class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	ApiResponse<List<UserInfo>> list(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(userService.list(me.outletId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<UserInfo> createCashier(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody CreateUserRequest request) {
		return ApiResponse.ok(userService.createCashier(me.outletId(), request));
	}

	@PutMapping("/{id}")
	ApiResponse<UserInfo> update(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id,
			@Valid @RequestBody UpdateUserRequest request) {
		return ApiResponse.ok(userService.update(me, id, request));
	}

}
