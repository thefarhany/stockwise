package com.thefarhany.stockwise.dto.auth;


public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
