package com.thefarhany.stockwise.dto.auth;


public record AuthResponse(UserInfo user, OutletInfo outlet, String accessToken, String refreshToken, long expiresIn) {
}
