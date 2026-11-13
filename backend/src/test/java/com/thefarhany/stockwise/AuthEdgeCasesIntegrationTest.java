package com.thefarhany.stockwise;

import static com.thefarhany.stockwise.ApiCases.get;
import static com.thefarhany.stockwise.ApiCases.post;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Auth: input aneh, token palsu/kedaluwarsa, refresh token kedaluwarsa, pendaftaran bersamaan. Commit sungguhan. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthEdgeCasesIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	TestClock clock;

	@Value("${stockwise.jwt.secret}")
	String secret;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	private static String reg(String email) {
		return "{\"name\":\"Budi\",\"email\":\"" + email + "\",\"password\":\"rahasia123\",\"outletName\":\"Warung\"}";
	}

	private static String email() {
		return "a-" + UUID.randomUUID() + "@x.id";
	}

	private String register(String email) throws Exception {
		return mvc.perform(post("/api/v1/auth/register", null, reg(email))).andReturn().getResponse().getContentAsString();
	}

	@Test
	void registerRejectsMalformedAndOutOfRangeInput() throws Exception {
		String ok = "\"password\":\"rahasia123\",\"outletName\":\"W\"";
		new ApiCases(mvc)
				.fail("body kosong", 400, "VALIDATION_ERROR", post("/api/v1/auth/register", null, "{}"))
				.fail("JSON rusak", 400, "VALIDATION_ERROR", post("/api/v1/auth/register", null, "{"))
				.fail("tanpa body", 400, "VALIDATION_ERROR", post("/api/v1/auth/register", null, null))
				.fail("sandi 7 karakter", 400, "VALIDATION_ERROR",
						post("/api/v1/auth/register", null, "{\"name\":\"B\",\"email\":\"" + email() + "\",\"password\":\"1234567\",\"outletName\":\"W\"}"))
				.fail("sandi 73 karakter (batas bcrypt)", 400, "VALIDATION_ERROR",
						post("/api/v1/auth/register", null, "{\"name\":\"B\",\"email\":\"" + email() + "\",\"password\":\"" + "a".repeat(73) + "\",\"outletName\":\"W\"}"))
				.fail("nama 101 karakter", 400, "VALIDATION_ERROR",
						post("/api/v1/auth/register", null, "{\"name\":\"" + "a".repeat(101) + "\",\"email\":\"" + email() + "\"," + ok + "}"))
				.fail("nama usaha 101 karakter", 400, "VALIDATION_ERROR",
						post("/api/v1/auth/register", null, "{\"name\":\"B\",\"email\":\"" + email() + "\",\"password\":\"rahasia123\",\"outletName\":\"" + "a".repeat(101) + "\"}"))
				.fail("email tanpa @", 400, "VALIDATION_ERROR",
						post("/api/v1/auth/register", null, "{\"name\":\"B\",\"email\":\"bukan-email\"," + ok + "}"))
				.fail("nama hanya spasi", 400, "VALIDATION_ERROR",
						post("/api/v1/auth/register", null, "{\"name\":\"   \",\"email\":\"" + email() + "\"," + ok + "}"))
				.ok("sandi tepat 72 karakter", 201,
						post("/api/v1/auth/register", null, "{\"name\":\"B\",\"email\":\"" + email() + "\",\"password\":\"" + "a".repeat(72) + "\",\"outletName\":\"W\"}"))
				.ok("nama berisi emoji dan tanda kutip", 201,
						post("/api/v1/auth/register", null, "{\"name\":\"Pak \\\"Budi\\\" 🍗\",\"email\":\"" + email() + "\"," + ok + "}"))
				.runAll();
	}

	@Test
	void loginAndRefreshRejectBadInput() throws Exception {
		String e = email();
		register(e);
		new ApiCases(mvc)
				.fail("email tidak dikenal", 401, "UNAUTHORIZED", post("/api/v1/auth/login", null, "{\"email\":\"" + email() + "\",\"password\":\"rahasia123\"}"))
				.fail("sandi salah", 401, "UNAUTHORIZED", post("/api/v1/auth/login", null, "{\"email\":\"" + e + "\",\"password\":\"salahsalah\"}"))
				.fail("body kosong", 400, "VALIDATION_ERROR", post("/api/v1/auth/login", null, "{}"))
				.fail("sandi kosong", 400, "VALIDATION_ERROR", post("/api/v1/auth/login", null, "{\"email\":\"" + e + "\",\"password\":\"\"}"))
				.ok("email huruf besar tetap dikenali", 200, post("/api/v1/auth/login", null, "{\"email\":\"" + e.toUpperCase() + "\",\"password\":\"rahasia123\"}"))
				.fail("refresh token sampah", 401, "UNAUTHORIZED", post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"sampah\"}"))
				.fail("refresh token kosong", 400, "VALIDATION_ERROR", post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"\"}"))
				.fail("logout tanpa login", 401, "UNAUTHORIZED", post("/api/v1/auth/logout", null, "{\"refreshToken\":\"x\"}"))
				.runAll();
	}

	@Test
	void logoutIsIdempotentAndIgnoresSomeoneElsesToken() throws Exception {
		String a = register(email());
		String b = register(email());
		String tokenA = JsonPath.read(a, "$.data.accessToken");
		String refreshA = JsonPath.read(a, "$.data.refreshToken");
		String tokenB = JsonPath.read(b, "$.data.accessToken");
		String refreshB = JsonPath.read(b, "$.data.refreshToken");

		// B mencoba mencabut refresh token milik A: diabaikan (A tetap bisa refresh)
		new ApiCases(mvc)
				.ok("logout dengan token milik orang lain", 200, post("/api/v1/auth/logout", tokenB, "{\"refreshToken\":\"" + refreshA + "\"}"))
				.ok("refresh milik A tetap hidup", 200, post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refreshA + "\"}"))
				.ok("logout sendiri", 200, post("/api/v1/auth/logout", tokenB, "{\"refreshToken\":\"" + refreshB + "\"}"))
				.ok("logout kedua kali tetap 200", 200, post("/api/v1/auth/logout", tokenB, "{\"refreshToken\":\"" + refreshB + "\"}"))
				.ok("logout dengan token tidak dikenal tetap 200", 200, post("/api/v1/auth/logout", tokenA, "{\"refreshToken\":\"tidak-dikenal\"}"))
				.fail("refresh B setelah logout", 401, "UNAUTHORIZED", post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refreshB + "\"}"))
				.runAll();
	}

	private String jwt(String subject, String outlet, String role, Instant issued, Instant expires, String key) {
		var builder = Jwts.builder().subject(subject).claim("outlet", outlet).claim("role", role)
				.issuedAt(Date.from(issued)).expiration(Date.from(expires));
		return key == null ? builder.compact()
				: builder.signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
	}

	@Test
	void forgedExpiredAndTamperedTokensAreRejected() throws Exception {
		String reg = register(email());
		String user = JsonPath.read(reg, "$.data.user.id");
		String outlet = JsonPath.read(reg, "$.data.outlet.id");
		Instant now = Instant.now();
		String good = jwt(user, outlet, "OWNER", now, now.plusSeconds(900), secret);
		String real = JsonPath.read(reg, "$.data.accessToken");
		String[] parts = real.split("\\.");
		// token milik CASHIER (sah), payload-nya diganti payload OWNER tanpa menandatangani ulang
		String cashierToken = jwt(user, outlet, "CASHIER", now, now.plusSeconds(900), secret);
		String[] cashierParts = cashierToken.split("\\.");
		String ownerPayload = good.split("\\.")[1];

		new ApiCases(mvc)
				.ok("token sah buatan sendiri", 200, get("/api/v1/me", good))
				.fail("token kedaluwarsa", 401, "TOKEN_EXPIRED",
						get("/api/v1/me", jwt(user, outlet, "OWNER", now.minusSeconds(2000), now.minusSeconds(1000), secret)))
				.fail("tanda tangan dari kunci lain", 401, "UNAUTHORIZED",
						get("/api/v1/me", jwt(user, outlet, "OWNER", now, now.plusSeconds(900), "kunci-lain-kunci-lain-kunci-lain-12345")))
				.fail("tanpa tanda tangan (alg none)", 401, "UNAUTHORIZED", get("/api/v1/me", jwt(user, outlet, "OWNER", now, now.plusSeconds(900), null)))
				.fail("isi diubah (peran CASHIER -> OWNER), tanda tangan lama", 401, "UNAUTHORIZED",
						get("/api/v1/users", cashierParts[0] + "." + ownerPayload + "." + cashierParts[2]))
				.fail("hanya dua bagian", 401, "UNAUTHORIZED", get("/api/v1/me", parts[0] + "." + parts[1]))
				.fail("kata 'Bearer' tanpa token", 401, "UNAUTHORIZED", get("/api/v1/me", "").header("Authorization", "Bearer "))
				.fail("skema Basic", 401, "UNAUTHORIZED", get("/api/v1/me", null).header("Authorization", "Basic abc"))
				.runAll();
	}

	@Test
	void refreshTokenExpiresAfterThirtyDays() throws Exception {
		clock.set("2026-11-02T04:00:00Z");
		String reg = register(email());
		String refresh = JsonPath.read(reg, "$.data.refreshToken");

		clock.set("2026-12-01T03:59:00Z"); // 29 hari 23 jam 59 menit: masih berlaku
		new ApiCases(mvc).ok("refresh sebelum 30 hari", 200, post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refresh + "\"}")).runAll();
		String second = mvc.perform(post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refresh + "\"}")).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<String>read(second, "$.errorCode")).as("token lama sudah dirotasi").isEqualTo("UNAUTHORIZED");

		String fresh = register(email());
		String refresh2 = JsonPath.read(fresh, "$.data.refreshToken");
		clock.set("2027-01-02T04:00:00Z"); // lebih dari 30 hari sejak token ke-2 dibuat
		new ApiCases(mvc).fail("refresh sesudah 30 hari", 401, "TOKEN_EXPIRED", post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refresh2 + "\"}")).runAll();
	}

	@Test
	void accountDeactivatedLosesAccessAtOnceOnProfileAndRefresh() throws Exception {
		String owner = register(email());
		String ownerToken = JsonPath.read(owner, "$.data.accessToken");
		String cashierEmail = email();
		String created = mvc.perform(post("/api/v1/users", ownerToken, "{\"name\":\"Siti\",\"email\":\"" + cashierEmail + "\",\"password\":\"kasir12345\"}"))
				.andReturn().getResponse().getContentAsString();
		String cashierId = JsonPath.read(created, "$.data.id");
		String login = mvc.perform(post("/api/v1/auth/login", null, "{\"email\":\"" + cashierEmail + "\",\"password\":\"kasir12345\"}"))
				.andReturn().getResponse().getContentAsString();
		String cashierToken = JsonPath.read(login, "$.data.accessToken");
		String cashierRefresh = JsonPath.read(login, "$.data.refreshToken");
		new ApiCases(mvc).ok("kasir aktif membuka profil", 200, get("/api/v1/me", cashierToken))
				.ok("kasir aktif membuka sesi", 200, get("/api/v1/sessions/current", cashierToken)).runAll();

		mvc.perform(ApiCases.put("/api/v1/users/" + cashierId, ownerToken, "{\"active\":false}"));
		new ApiCases(mvc)
				.fail("profil setelah dinonaktifkan", 401, "UNAUTHORIZED", get("/api/v1/me", cashierToken))
				// access token masih belum habis, tapi endpoint mana pun langsung menolak
				.fail("sesi setelah dinonaktifkan", 401, "UNAUTHORIZED", get("/api/v1/sessions/current", cashierToken))
				.fail("refresh setelah dinonaktifkan", 401, "UNAUTHORIZED", post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + cashierRefresh + "\"}"))
				.fail("login setelah dinonaktifkan", 401, "UNAUTHORIZED", post("/api/v1/auth/login", null, "{\"email\":\"" + cashierEmail + "\",\"password\":\"kasir12345\"}"))
				.runAll();
	}

	@Test
	void simultaneousRegistrationsWithTheSameEmailCreateExactlyOneAccount() throws Exception {
		String e = email();
		ExecutorService pool = Executors.newFixedThreadPool(6);
		CountDownLatch go = new CountDownLatch(1);
		List<Future<Integer>> results = new java.util.ArrayList<>();
		for (int i = 0; i < 6; i++) {
			Callable<Integer> task = () -> {
				go.await();
				var response = mvc.perform(post("/api/v1/auth/register", null, reg(e))).andReturn().getResponse();
				String code = response.getStatus() == 201 ? "-" : JsonPath.read(response.getContentAsString(), "$.errorCode");
				assertThat(code).isIn("-", "EMAIL_TAKEN", "DATA_CONFLICT"); // kalah balapan: ditangkap UNIQUE
				return response.getStatus();
			};
			results.add(pool.submit(task));
		}
		go.countDown();
		List<Integer> statuses = new java.util.ArrayList<>();
		for (Future<Integer> f : results) {
			statuses.add(f.get());
		}
		pool.shutdown();
		assertThat(statuses.stream().filter(s -> s == 201).count()).as("tepat satu akun berhasil").isEqualTo(1);
		assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(5);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email = ?", Integer.class, e)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outlet o JOIN app_user u ON u.outlet_id = o.id WHERE u.email = ?",
				Integer.class, e)).as("tidak ada usaha yatim dari pendaftaran yang kalah").isEqualTo(1);
	}

}
