package com.thefarhany.stockwise.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Dipakai untuk refresh dan logout. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
