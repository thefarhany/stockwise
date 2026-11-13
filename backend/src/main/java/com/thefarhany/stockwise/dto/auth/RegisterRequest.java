package com.thefarhany.stockwise.dto.auth;

import com.thefarhany.stockwise.model.auth.Language;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** language kosong = EN. */
public record RegisterRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Email @Size(max = 150) String email,
		@NotBlank @Size(min = 8, max = 72) String password, // 72 = batas byte bcrypt
		@NotBlank @Size(max = 100) String outletName,
		Language language) {
}
