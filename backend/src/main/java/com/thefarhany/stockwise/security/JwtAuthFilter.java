package com.thefarhany.stockwise.security;

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

	JwtAuthFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			try {
				// shortcut: akun yang dinonaktifkan masih lolos sampai access token habis (15 menit)
				AuthPrincipal principal = jwtService.parse(header.substring(7));
				SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
						principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))));
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
