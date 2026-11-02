package com.thefarhany.stockwise.service.auth;

import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.auth.Role;
import com.thefarhany.stockwise.security.AuthPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	public static final long ACCESS_TTL_SECONDS = 900;

	private final SecretKey key;

	public JwtService(@Value("${stockwise.jwt.secret}") String secret) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("stockwise.jwt.secret (env JWT_SECRET) minimal 32 karakter");
		}
		this.key = Keys.hmacShaKeyFor(bytes);
	}

	public String createAccessToken(AppUser user) {
		// jam sistem, bukan Clock aplikasi: jjwt memvalidasi exp dengan jam sistem juga
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getId().toString())
				.claim("outlet", user.getOutlet().getId().toString())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(ACCESS_TTL_SECONDS)))
				.signWith(key)
				.compact();
	}

	/** Melempar JwtException (atau ExpiredJwtException) kalau token tidak valid. */
	public AuthPrincipal parse(String token) {
		Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		return new AuthPrincipal(UUID.fromString(claims.getSubject()),
				UUID.fromString(claims.get("outlet", String.class)), Role.valueOf(claims.get("role", String.class)));
	}

}
