package com.thefarhany.stockwise.security;

import com.thefarhany.stockwise.model.auth.Role;
import java.util.UUID;

/** Identitas dari access token; dipakai lewat @AuthenticationPrincipal. outletId selalu dari sini, bukan dari request. */
public record AuthPrincipal(UUID userId, UUID outletId, Role role) {
}
