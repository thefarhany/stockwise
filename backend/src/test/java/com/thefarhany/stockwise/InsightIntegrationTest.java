package com.thefarhany.stockwise;

import static com.thefarhany.stockwise.ShopFixture.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.thefarhany.stockwise.ShopFixture.Shop;
import com.thefarhany.stockwise.service.session.SessionService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tanpa @Transactional. Ollama diganti FakeOllama (port tetap dari application-test.properties), jadi seluruh
 * jalur HTTP, parsing, dan validasi angka diuji tanpa model sungguhan.
 * Hari uji: 5 porsi Ayam Goreng terjual = omzet Rp65.000, biaya Rp39.000, laba Rp26.000.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class InsightIntegrationTest {

	private static final String GOOD_TEXT = "Hari ini laku 5 porsi dengan omzet Rp65.000 dan laba Rp26.000.";

	@Autowired
	MockMvc mvc;

	@Autowired
	TestClock clock;

	@Autowired
	SessionService sessionService;

	private ShopFixture fixture;
	private FakeOllama ollama;

	@BeforeEach
	void setUp() throws Exception {
		fixture = new ShopFixture(mvc);
		ollama = new FakeOllama();
	}

	@AfterEach
	void tearDown() {
		ollama.close();
		clock.reset();
	}

	private String insight(Shop s, String sessionId) throws Exception {
		return fixture.json(as(post("/api/v1/sessions/" + sessionId + "/insight"), s.owner(), ""));
	}

	@Test
	void validModelTextIsSavedAndRequestCarriesOnlyAggregateFacts() throws Exception {
		Shop s = fixture.create();
		String sid = fixture.closedDay(s, clock, 5);
		ollama.replyWith(GOOD_TEXT);

		String body = insight(s, sid);
		assertThat(JsonPath.<String>read(body, "$.data.status")).isEqualTo("READY");
		assertThat(JsonPath.<String>read(body, "$.data.text")).isEqualTo(GOOD_TEXT);
		assertThat(JsonPath.<String>read(body, "$.data.model")).isEqualTo("test-model");
		assertThat(JsonPath.<Integer>read(body, "$.data.attemptsLeft")).isEqualTo(2);

		// hasilnya tersimpan; membacanya tidak memakai kesempatan
		for (int i = 0; i < 2; i++) {
			mvc.perform(as(get("/api/v1/sessions/" + sid + "/summary"), s.owner(), ""))
					.andExpect(jsonPath("$.data.insight.status").value("READY"))
					.andExpect(jsonPath("$.data.insight.text").value(GOOD_TEXT))
					.andExpect(jsonPath("$.data.insight.model").value("test-model"))
					.andExpect(jsonPath("$.data.insight.attemptsLeft").value(2));
		}

		// yang dikirim ke model: model, instruksi, format, kunci API, dan hanya fakta agregat
		String sent = ollama.lastBody.get();
		assertThat(JsonPath.<String>read(sent, "$.model")).isEqualTo("test-model");
		assertThat(JsonPath.<Boolean>read(sent, "$.stream")).isFalse();
		assertThat(JsonPath.<Object>read(sent, "$.format.properties.text")).isNotNull();
		String facts = JsonPath.read(sent, "$.messages[1].content"); // fakta ikut sebagai teks JSON di dalam pesan
		assertThat(facts).contains("FAKTA", "\"revenue\":65000", "\"netProfit\":26000", "\"itemsSold\":5",
				"Geprek Pak Budi", "\"closeType\":\"MANUAL\"");
		assertThat(sent).doesNotContain("@", "Siti", "email", "password", "Bearer");
		assertThat(ollama.lastAuthorization.get()).isEqualTo("Bearer test-key");
	}

	@Test
	void textWithNumbersNotInFactsFallsBackToTemplate() throws Exception {
		Shop s = fixture.create();
		String sid = fixture.closedDay(s, clock, 5);
		ollama.replyWith("Omzet hari ini Rp99.000, jauh di atas biasanya.");

		String body = insight(s, sid);
		assertThat(JsonPath.<String>read(body, "$.data.status")).isEqualTo("READY");
		assertThat(JsonPath.<String>read(body, "$.data.model")).isEqualTo("template");
		assertThat(JsonPath.<String>read(body, "$.data.text")).contains("5 porsi", "Rp65.000", "Rp26.000")
				.doesNotContain("99.000");
		assertThat(JsonPath.<Integer>read(body, "$.data.attemptsLeft")).isEqualTo(2); // tetap memakai satu kesempatan
	}

	@Test
	void modelErrorOrTimeoutFallsBackToTemplateWithoutError() throws Exception {
		Shop s = fixture.create();
		String sid = fixture.closedDay(s, clock, 5);

		ollama.failWith(500);
		assertThat(JsonPath.<String>read(insight(s, sid), "$.data.model")).isEqualTo("template");

		ollama.replyWith(GOOD_TEXT).delay(3500); // lebih lama dari timeout 2 detik
		String slow = insight(s, sid);
		assertThat(JsonPath.<String>read(slow, "$.data.model")).isEqualTo("template");
		assertThat(JsonPath.<Integer>read(slow, "$.data.attemptsLeft")).isEqualTo(1);
	}

	@Test
	void autoClosedSessionTemplateMentionsAutomaticCount() throws Exception {
		Shop s = fixture.create();
		clock.set("2026-11-02T04:00:00Z");
		String sid = fixture.open(s.owner(), s);
		clock.set("2026-11-02T05:00:00Z");
		fixture.sell(s.cashier(), sid, s.goreng(), 5);
		clock.set("2026-11-02T14:30:00Z");
		sessionService.autoCloseDue();
		ollama.failWith(500);

		assertThat(JsonPath.<String>read(insight(s, sid), "$.data.text")).contains("otomatis");
	}

	@Test
	void onlyThreeAttemptsPerSession() throws Exception {
		Shop s = fixture.create();
		String sid = fixture.closedDay(s, clock, 5);
		ollama.replyWith(GOOD_TEXT);
		for (int expectedLeft = 2; expectedLeft >= 0; expectedLeft--) {
			assertThat(JsonPath.<Integer>read(insight(s, sid), "$.data.attemptsLeft")).isEqualTo(expectedLeft);
		}
		int callsBefore = ollama.calls.get();
		mvc.perform(as(post("/api/v1/sessions/" + sid + "/insight"), s.owner(), ""))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.errorCode").value("INSIGHT_LIMIT_REACHED"));
		assertThat(ollama.calls.get()).as("model tidak dipanggil lagi").isEqualTo(callsBefore);
		mvc.perform(as(get("/api/v1/sessions/" + sid + "/summary"), s.owner(), ""))
				.andExpect(jsonPath("$.data.insight.attemptsLeft").value(0))
				.andExpect(jsonPath("$.data.insight.text").value(GOOD_TEXT)); // hasil terakhir tetap ada
	}

	@Test
	void busyModelAnswers503WithoutConsumingAnAttempt() throws Exception {
		Shop s = fixture.create();
		String sid = fixture.closedDay(s, clock, 5);
		ollama.replyWith(GOOD_TEXT).delay(1500);

		CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> {
			try {
				return insight(s, sid);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
		long deadline = System.currentTimeMillis() + 5000;
		while (ollama.calls.get() == 0 && System.currentTimeMillis() < deadline) {
			Thread.sleep(20);
		}
		mvc.perform(as(post("/api/v1/sessions/" + sid + "/insight"), s.owner(), ""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.errorCode").value("INSIGHT_BUSY"));

		assertThat(JsonPath.<Integer>read(first.get(), "$.data.attemptsLeft")).isEqualTo(2);
		mvc.perform(as(get("/api/v1/sessions/" + sid + "/summary"), s.owner(), ""))
				.andExpect(jsonPath("$.data.insight.attemptsLeft").value(2));
		assertThat(ollama.calls.get()).isEqualTo(1);
	}

	@Test
	void insightRulesForRolesSessionStateAndTenancy() throws Exception {
		Shop a = fixture.create();
		Shop b = fixture.create();
		String closed = fixture.closedDay(a, clock, 5);
		clock.set("2026-11-03T04:00:00Z");
		String open = fixture.open(b.owner(), b);

		mvc.perform(as(post("/api/v1/sessions/" + closed + "/insight"), a.cashier(), ""))
				.andExpect(status().isForbidden());
		mvc.perform(as(post("/api/v1/sessions/" + closed + "/insight"), b.owner(), ""))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));
		mvc.perform(as(post("/api/v1/sessions/" + UUID.randomUUID() + "/insight"), a.owner(), ""))
				.andExpect(status().isNotFound());
		mvc.perform(as(post("/api/v1/sessions/" + open + "/insight"), b.owner(), ""))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_CLOSED"));
		assertThat(ollama.calls.get()).as("tidak ada panggilan model untuk permintaan yang ditolak").isZero();
	}

}
