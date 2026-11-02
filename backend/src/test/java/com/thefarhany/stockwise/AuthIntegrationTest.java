package com.thefarhany.stockwise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthIntegrationTest {

	@Autowired
	MockMvc mvc;

	private static String register(String email, String outlet) {
		return "{\"name\":\"Budi\",\"email\":\"" + email + "\",\"password\":\"rahasia123\",\"outletName\":\""
				+ outlet + "\"}";
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body) {
		return b.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private String call(MockHttpServletRequestBuilder b) throws Exception {
		return mvc.perform(b).andReturn().getResponse().getContentAsString();
	}

	private static String read(String body, String path) {
		return JsonPath.read(body, path);
	}

	private static MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder b, String token) {
		return b.header("Authorization", "Bearer " + token);
	}

	@Test
	void registerLoginMeRefreshRotationAndLogout() throws Exception {
		String registered = mvc.perform(json(post("/api/v1/auth/register"), register("Budi@Example.com", "Geprek")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.user.role").value("OWNER"))
				.andExpect(jsonPath("$.data.user.email").value("budi@example.com"))
				.andExpect(jsonPath("$.data.outlet.setupCompleted").value(false))
				.andExpect(jsonPath("$.data.expiresIn").value(900))
				.andReturn().getResponse().getContentAsString();

		String login = call(json(post("/api/v1/auth/login"),
				"{\"email\":\"budi@example.com\",\"password\":\"rahasia123\"}"));
		mvc.perform(bearer(get("/api/v1/me"), read(login, "$.data.accessToken")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.name").value("Budi"))
				.andExpect(jsonPath("$.data.outlet.name").value("Geprek"));

		String oldRefresh = read(registered, "$.data.refreshToken");
		String refreshed = call(json(post("/api/v1/auth/refresh"), "{\"refreshToken\":\"" + oldRefresh + "\"}"));
		String newRefresh = read(refreshed, "$.data.refreshToken");

		mvc.perform(json(post("/api/v1/auth/refresh"), "{\"refreshToken\":\"" + oldRefresh + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

		mvc.perform(json(bearer(post("/api/v1/auth/logout"), read(refreshed, "$.data.accessToken")),
				"{\"refreshToken\":\"" + newRefresh + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value((Object) null));
		mvc.perform(json(post("/api/v1/auth/refresh"), "{\"refreshToken\":\"" + newRefresh + "\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void duplicateEmailAndBadCredentialsAndValidation() throws Exception {
		mvc.perform(json(post("/api/v1/auth/register"), register("a@x.id", "A"))).andExpect(status().isCreated());
		mvc.perform(json(post("/api/v1/auth/register"), register("A@X.id", "B")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("EMAIL_TAKEN"));
		mvc.perform(json(post("/api/v1/auth/login"), "{\"email\":\"a@x.id\",\"password\":\"salahsalah\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
		mvc.perform(json(post("/api/v1/auth/register"),
				"{\"name\":\"\",\"email\":\"bukan-email\",\"password\":\"pendek\",\"outletName\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.details.fields.email").exists())
				.andExpect(jsonPath("$.details.fields.password").exists());
	}

	@Test
	void protectedEndpointsNeedValidToken() throws Exception {
		mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
		mvc.perform(bearer(get("/api/v1/me"), "bukan.token.valid")).andExpect(status().isUnauthorized());
	}

	@Test
	void ownerManagesCashierAndCashierIsForbidden() throws Exception {
		String owner = call(json(post("/api/v1/auth/register"), register("o@x.id", "Warung")));
		String ownerToken = read(owner, "$.data.accessToken");

		String created = mvc.perform(json(bearer(post("/api/v1/users"), ownerToken),
				"{\"name\":\"Siti\",\"email\":\"siti@x.id\",\"password\":\"kasir12345\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.role").value("CASHIER"))
				.andReturn().getResponse().getContentAsString();
		String cashierId = read(created, "$.data.id");

		mvc.perform(json(bearer(post("/api/v1/users"), ownerToken),
				"{\"name\":\"Dobel\",\"email\":\"SITI@x.id\",\"password\":\"kasir12345\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("EMAIL_TAKEN"));

		String cashier = call(json(post("/api/v1/auth/login"),
				"{\"email\":\"siti@x.id\",\"password\":\"kasir12345\"}"));
		String cashierToken = read(cashier, "$.data.accessToken");
		mvc.perform(bearer(get("/api/v1/users"), cashierToken)).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		mvc.perform(bearer(get("/api/v1/me"), cashierToken)).andExpect(status().isOk());

		mvc.perform(bearer(get("/api/v1/users"), ownerToken))
				.andExpect(jsonPath("$.data.length()").value(2));

		// nonaktifkan: tidak bisa login dan refresh token lama dicabut
		mvc.perform(json(bearer(put("/api/v1/users/" + cashierId), ownerToken), "{\"active\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.active").value(false));
		mvc.perform(json(post("/api/v1/auth/login"), "{\"email\":\"siti@x.id\",\"password\":\"kasir12345\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(json(post("/api/v1/auth/refresh"),
				"{\"refreshToken\":\"" + read(cashier, "$.data.refreshToken") + "\"}"))
				.andExpect(status().isUnauthorized());

		// owner tidak bisa menonaktifkan diri sendiri
		mvc.perform(json(bearer(put("/api/v1/users/" + read(owner, "$.data.user.id")), ownerToken),
				"{\"active\":false}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void outletsAreIsolatedBetweenRegistrations() throws Exception {
		String a = call(json(post("/api/v1/auth/register"), register("a1@x.id", "Usaha A")));
		String b = call(json(post("/api/v1/auth/register"), register("b1@x.id", "Usaha B")));
		String tokenA = read(a, "$.data.accessToken");
		String tokenB = read(b, "$.data.accessToken");

		mvc.perform(bearer(get("/api/v1/me"), tokenB))
				.andExpect(jsonPath("$.data.outlet.name").value("Usaha B"));
		mvc.perform(json(bearer(post("/api/v1/users"), tokenA),
				"{\"name\":\"Kasir A\",\"email\":\"ka@x.id\",\"password\":\"kasir12345\"}")).andExpect(status().isCreated());
		mvc.perform(bearer(get("/api/v1/users"), tokenB)).andExpect(jsonPath("$.data.length()").value(1));

		// owner B tidak bisa menyentuh akun milik usaha A
		String cashierA = JsonPath.<java.util.List<String>>read(
				call(bearer(get("/api/v1/users"), tokenA)), "$.data[?(@.role=='CASHIER')].id").get(0);
		mvc.perform(json(bearer(put("/api/v1/users/" + cashierA), tokenB), "{\"name\":\"Hack\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

}
