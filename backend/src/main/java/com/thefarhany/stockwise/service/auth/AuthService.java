package com.thefarhany.stockwise.service.auth;

import com.thefarhany.stockwise.dto.auth.AuthResponse;
import com.thefarhany.stockwise.dto.auth.LoginRequest;
import com.thefarhany.stockwise.dto.auth.OutletInfo;
import com.thefarhany.stockwise.dto.auth.ProfileResponse;
import com.thefarhany.stockwise.dto.auth.RegisterRequest;
import com.thefarhany.stockwise.dto.auth.TokenResponse;
import com.thefarhany.stockwise.dto.auth.UserInfo;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.auth.RefreshToken;
import com.thefarhany.stockwise.model.auth.Role;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.repository.auth.AppUserRepository;
import com.thefarhany.stockwise.repository.auth.RefreshTokenRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Duration REFRESH_TTL = Duration.ofDays(30);
	private static final SecureRandom RANDOM = new SecureRandom();

	private final OutletRepository outlets;
	private final AppUserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder encoder;
	private final JwtService jwt;
	private final Clock clock;

	public AuthService(OutletRepository outlets, AppUserRepository users, RefreshTokenRepository refreshTokens,
			PasswordEncoder encoder, JwtService jwt, Clock clock) {
		this.clock = clock;
		this.outlets = outlets;
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.encoder = encoder;
		this.jwt = jwt;
	}

	/** Setiap pendaftaran membuat usaha baru (multi-tenant). Race email ditangkap UNIQUE -> DATA_CONFLICT. */
	@Transactional
	public AuthResponse register(RegisterRequest req) {
		String email = normalize(req.email());
		if (users.existsByEmail(email)) {
			throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email sudah terdaftar.");
		}
		Outlet outlet = outlets.save(new Outlet(req.outletName().trim()));
		AppUser owner = users.save(new AppUser(outlet, req.name().trim(), email, encoder.encode(req.password()),
				Role.OWNER));
		return authResponse(owner);
	}

	@Transactional
	public AuthResponse login(LoginRequest req) {
		AppUser user = users.findByEmail(normalize(req.email()))
				.filter(u -> u.isActive() && encoder.matches(req.password(), u.getPasswordHash()))
				.orElseThrow(() -> unauthorized("Email atau password salah."));
		return authResponse(user);
	}

	@Transactional
	public TokenResponse refresh(String rawToken) {
		String hash = hash(rawToken);
		RefreshToken stored = refreshTokens.findByTokenHash(hash)
				.orElseThrow(() -> unauthorized("Refresh token tidak valid."));
		Instant now = Instant.now(clock);
		if (stored.getRevokedAt() == null && !stored.getExpiresAt().isAfter(now)) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Refresh token kedaluwarsa.");
		}
		AppUser user = stored.getUser();
		if (!user.isActive() || refreshTokens.revokeIfActive(hash, now) == 0) {
			throw unauthorized("Refresh token tidak valid.");
		}
		return new TokenResponse(jwt.createAccessToken(user), issueRefreshToken(user), JwtService.ACCESS_TTL_SECONDS);
	}

	/** Idempoten: token tidak dikenal atau milik user lain diabaikan. */
	@Transactional
	public void logout(UUID userId, String rawToken) {
		String hash = hash(rawToken);
		refreshTokens.findByTokenHash(hash)
				.filter(t -> t.getUser().getId().equals(userId))
				.ifPresent(t -> refreshTokens.revokeIfActive(hash, Instant.now(clock)));
	}

	@Transactional(readOnly = true)
	public ProfileResponse profile(UUID userId) {
		return users.findById(userId).filter(AppUser::isActive).map(ProfileResponse::from)
				.orElseThrow(() -> unauthorized("Perlu login."));
	}

	private AuthResponse authResponse(AppUser user) {
		return new AuthResponse(UserInfo.from(user), OutletInfo.from(user.getOutlet()),
				jwt.createAccessToken(user), issueRefreshToken(user), JwtService.ACCESS_TTL_SECONDS);
	}

	private String issueRefreshToken(AppUser user) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		refreshTokens.save(new RefreshToken(user, hash(raw), Instant.now(clock).plus(REFRESH_TTL)));
		return raw;
	}

	private static String hash(String raw) {
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static BusinessException unauthorized(String message) {
		return new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
	}

}
