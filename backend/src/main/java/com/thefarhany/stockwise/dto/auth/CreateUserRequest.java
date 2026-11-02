package com.thefarhany.stockwise.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Akun yang dibuat Owner selalu berperan CASHIER. */
public record CreateUserRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Email @Size(max = 150) String email,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
