package com.thefarhany.stockwise.security;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.repository.auth.AppUserRepository;
import com.thefarhany.stockwise.service.auth.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, RateLimiter rateLimiter,
			ObjectMapper mapper, AppUserRepository users) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
								"/api/v1/auth/refresh").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(new AuthRateLimitFilter(rateLimiter, mapper), UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(new JwtAuthFilter(jwtService, users), UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(e -> e
						.authenticationEntryPoint((req, res, ex) -> {
							boolean expired = req.getAttribute(JwtAuthFilter.EXPIRED_ATTR) != null;
							write(res, mapper, 401, expired
									? ApiResponse.error("TOKEN_EXPIRED", "Token kedaluwarsa.", null)
									: ApiResponse.error("UNAUTHORIZED", "Perlu login.", null));
						})
						.accessDeniedHandler((req, res, ex) ->
								write(res, mapper, 403, ApiResponse.error("FORBIDDEN", "Tidak punya akses.", null))))
				.build();
	}

	private static void write(HttpServletResponse res, ObjectMapper mapper, int status, ApiResponse<Void> body)
			throws IOException {
		res.setStatus(status);
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		mapper.writeValue(res.getOutputStream(), body);
	}

}
