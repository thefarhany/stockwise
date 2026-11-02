package com.thefarhany.stockwise;

import static com.thefarhany.stockwise.ApiCases.post;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Batas percobaan login/daftar per IP diuji lewat endpoint asli (konteks sendiri dengan batas 3). */
@SpringBootTest(properties = "stockwise.rate-limit.auth-max=3")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RateLimitIntegrationTest {

	@Autowired
	MockMvc mvc;

	private MockHttpServletResponse login(String ip) throws Exception {
		return mvc.perform(post("/api/v1/auth/login", null,
				"{\"email\":\"x-" + UUID.randomUUID() + "@x.id\",\"password\":\"salah12345\"}")
				.with(r -> {
					r.setRemoteAddr(ip);
					return r;
				})).andReturn().getResponse();
	}

	@Test
	void fourthAttemptFromSameIpIsBlockedWhileOtherIpIsNot() throws Exception {
		for (int i = 0; i < 3; i++) {
			assertThat(login("10.0.0.1").getStatus()).as("percobaan %d", i + 1).isEqualTo(401);
		}
		MockHttpServletResponse blocked = login("10.0.0.1");
		assertThat(blocked.getStatus()).isEqualTo(429);
		assertThat(blocked.getContentAsString()).contains("TOO_MANY_REQUESTS");
		assertThat(blocked.getHeader("Retry-After")).isNotNull();
		assertThat(Integer.parseInt(blocked.getHeader("Retry-After"))).isBetween(1, 60);
		assertThat(login("10.0.0.2").getStatus()).as("IP lain tidak ikut terblokir").isEqualTo(401);
	}

	@Test
	void registerSharesTheSameBudgetPerIp() throws Exception {
		for (int i = 0; i < 3; i++) {
			mvc.perform(post("/api/v1/auth/register", null, "{}").with(r -> {
				r.setRemoteAddr("10.0.0.9");
				return r;
			}));
		}
		MockHttpServletResponse blocked = mvc.perform(post("/api/v1/auth/register", null, "{}").with(r -> {
			r.setRemoteAddr("10.0.0.9");
			return r;
		})).andReturn().getResponse();
		assertThat(blocked.getStatus()).isEqualTo(429);
	}

}
