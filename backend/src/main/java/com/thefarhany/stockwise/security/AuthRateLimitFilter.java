package com.thefarhany.stockwise.security;

import com.thefarhany.stockwise.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Membatasi percobaan login dan daftar per IP (TOO_MANY_REQUESTS, NFR-04). */
class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> LIMITED = Set.of("/api/v1/auth/login", "/api/v1/auth/register");

	private final RateLimiter limiter;
	private final ObjectMapper mapper;

	AuthRateLimitFilter(RateLimiter limiter, ObjectMapper mapper) {
		this.limiter = limiter;
		this.mapper = mapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equals(request.getMethod()) || !LIMITED.contains(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		// getRemoteAddr sudah IP klien asli: RemoteIpValve membacanya dari X-Forwarded-For (application.properties)
		long retryAfter = limiter.tryAcquire(request.getRemoteAddr() + request.getRequestURI());
		if (retryAfter == 0) {
			chain.doFilter(request, response);
			return;
		}
		response.setStatus(429);
		response.setHeader("Retry-After", String.valueOf(retryAfter));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		mapper.writeValue(response.getOutputStream(), ApiResponse.error("TOO_MANY_REQUESTS",
				"Terlalu banyak percobaan. Coba lagi nanti.", Map.of("retryAfterSeconds", retryAfter)));
	}

}
