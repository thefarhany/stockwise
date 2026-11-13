package com.thefarhany.stockwise.security;

import com.thefarhany.stockwise.repository.auth.AppUserRepository;
import com.thefarhany.stockwise.service.auth.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class JwtAuthFilter extends OncePerRequestFilter {

	static final String EXPIRED_ATTR = "jwt.expired";

	private final JwtService jwtService;
	private final AppUserRepository users;

	JwtAuthFilter(JwtService jwtService, AppUserRepository users) {
		this.jwtService = jwtService;
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			try {
				AuthPrincipal principal = jwtService.parse(header.substring(7));
				// Akun yang dinonaktifkan atau dihapus langsung ditolak, tidak menunggu access token habis (satu query PK).
				if (users.existsByIdAndActiveTrue(principal.userId())) {
					SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
							principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))));
				}
			}
			catch (ExpiredJwtException ex) {
				request.setAttribute(EXPIRED_ATTR, true);
			}
			catch (JwtException | IllegalArgumentException ex) {
				// token rusak: biarkan tidak terautentikasi, entry point menjawab 401
			}
		}
		chain.doFilter(request, response);
	}

}
