package com.thefarhany.stockwise;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Login lewat BFF datang dari satu alamat (server Next.js). Batas per IP harus memakai IP klien di X-Forwarded-For,
 * yang dibaca Tomcat RemoteIpValve. MockMvc tidak melewati valve itu, jadi diuji lewat server sungguhan.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "stockwise.rate-limit.auth-max=3")
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ForwardedForRateLimitIntegrationTest {

	@LocalServerPort
	int port;

	private final HttpClient http = HttpClient.newHttpClient();

	private int login(String forwardedFor) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
				.header("Content-Type", "application/json")
				.header("X-Forwarded-For", forwardedFor)
				.POST(HttpRequest.BodyPublishers.ofString(
						"{\"email\":\"x-" + UUID.randomUUID() + "@x.id\",\"password\":\"salah12345\"}"))
				.build();
		return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
	}

	@Test
	void clientsBehindTheSameProxyHaveSeparateBudgets() throws Exception {
		for (int i = 0; i < 3; i++) {
			assertThat(login("203.0.113.10")).as("percobaan %d", i + 1).isEqualTo(401);
		}
		assertThat(login("203.0.113.10")).isEqualTo(429);
		assertThat(login("203.0.113.20")).as("klien lain lewat proxy yang sama").isEqualTo(401);
	}

	@Test
	void spoofedLeftmostEntryDoesNotEscapeTheLimit() throws Exception {
		// Proxy menambahkan IP asli di kanan; isian palsu dari klien di kiri tidak boleh membuat bucket baru.
		for (int i = 0; i < 3; i++) {
			login("198.51.100." + i + ", 203.0.113.30");
		}
		assertThat(login("198.51.100.99, 203.0.113.30")).isEqualTo(429);
	}

}
