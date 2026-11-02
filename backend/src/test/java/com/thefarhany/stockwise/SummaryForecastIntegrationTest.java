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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Tanpa @Transactional: tiap request commit/rollback sungguhan. Tiap tes memakai usaha sendiri. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SummaryForecastIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	TestClock clock;

	@Autowired
	SessionService sessionService;

	private ShopFixture fixture;

	@BeforeEach
	void setUp() {
		fixture = new ShopFixture(mvc);
	}

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	private String json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b) throws Exception {
		return fixture.json(b);
	}

	private static double num(String json, String path) {
		return ((Number) JsonPath.read(json, path)).doubleValue();
	}

	@Test
	void summaryCapturesTheDayAndRecommendationCorrectsForSoldOutItem() throws Exception {
		Shop s = fixture.create();
		clock.set("2026-11-02T04:00:00Z"); // 11:00 WIB
		String sid = fixture.open(s.owner(), s);
		clock.set("2026-11-02T05:00:00Z");
		String mistake = fixture.sell(s.cashier(), sid, s.goreng(), 1);
		mvc.perform(as(post("/api/v1/sales/" + mistake + "/void"), s.cashier(), "{\"reason\":\"Salah tekan\"}"))
				.andExpect(status().isOk());
		clock.set("2026-11-02T11:40:00Z"); // 18:40 WIB: ayam habis
		fixture.sell(s.cashier(), sid, s.goreng(), 10);
		clock.set("2026-11-02T14:00:00Z");

		// sebelum ditutup: belum ada ringkasan
		mvc.perform(as(get("/api/v1/sessions/" + sid + "/summary"), s.owner(), ""))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_CLOSED"));
		fixture.close(s.owner(), sid, s, "0", "1000", "5");

		String summary = json(as(get("/api/v1/sessions/" + sid + "/summary"), s.owner(), ""));
		assertThat(num(summary, "$.data.revenue")).isEqualTo(130000);
		assertThat(num(summary, "$.data.cogs")).isEqualTo(78000); // ayam 10 x 7.000 + tepung 1.000 g x 8
		assertThat(num(summary, "$.data.wasteCost")).isZero();
		assertThat(num(summary, "$.data.adjustmentCost")).isZero();
		assertThat(num(summary, "$.data.carriedValue")).isEqualTo(8000);
		assertThat(num(summary, "$.data.expenseTotal")).isZero();
		assertThat(num(summary, "$.data.netProfit")).isEqualTo(52000);
		assertThat(num(summary, "$.data.totalItemsSold")).isEqualTo(10);
		assertThat(num(summary, "$.data.voidCount")).isEqualTo(1);
		assertThat(JsonPath.<String>read(summary, "$.data.closeType")).isEqualTo("MANUAL");
		assertThat(JsonPath.<String>read(summary, "$.data.primaryItem.name")).isEqualTo("Ayam");
		assertThat(JsonPath.<String>read(summary, "$.data.primaryItem.soldOutAt")).isEqualTo("2026-11-02T11:40:00Z");
		assertThat(num(summary, "$.data.primaryItem.lostSalesEstimate")).isEqualTo(3.0); // 10 x 140 / 460 menit
		assertThat(JsonPath.<String>read(summary, "$.data.insight.status")).isEqualTo("NONE");
		assertThat(num(summary, "$.data.insight.attemptsLeft")).isEqualTo(3);
		assertThat(JsonPath.<String>read(summary, "$.data.nextDayRecommendationDate")).isEqualTo("2026-11-03");

		// saran untuk besok: permintaan 10 + 3 terlewat = 13, plus buffer 5%
		clock.set("2026-11-03T01:00:00Z"); // 08:00 WIB tanggal 3
		String rec = json(as(get("/api/v1/recommendations"), s.owner(), "")); // tanpa date = hari ini
		assertThat(JsonPath.<String>read(rec, "$.data.targetDate")).isEqualTo("2026-11-03");
		assertThat(JsonPath.<String>read(rec, "$.data.method")).isEqualTo("COLD_START");
		assertThat(num(rec, "$.data.dataPoints")).isEqualTo(1);
		assertThat(JsonPath.<Boolean>read(rec, "$.data.lowData")).isTrue();
		assertThat(JsonPath.<List<Object>>read(rec, "$.data.items")).hasSize(1); // hanya item utama
		assertThat(JsonPath.<String>read(rec, "$.data.items[0].name")).isEqualTo("Ayam");
		assertThat(num(rec, "$.data.items[0].requiredQty")).isEqualTo(13);
		assertThat(num(rec, "$.data.items[0].carryOverQty")).isZero();
		assertThat(num(rec, "$.data.items[0].recommendedQty")).isEqualTo(14); // 13 x 1,05 = 13,65
		assertThat(num(rec, "$.data.items[0].lowerQty")).isEqualTo(11); // 10 x 1,05
		assertThat(num(rec, "$.data.items[0].upperQty")).isEqualTo(17); // 16 x 1,05 = 16,8
		assertThat(num(rec, "$.data.items[0].estimatedCost")).isEqualTo(98000);
		assertThat(JsonPath.<List<String>>read(rec, "$.data.items[0].reasons[*].code"))
				.containsExactly("STOCKOUT_CORRECTION", "LOW_DATA");
		assertThat(JsonPath.<String>read(rec, "$.data.items[0].reasons[0].text"))
				.isEqualTo("Kemarin habis jam 18.40, sekitar 3 pembeli terlewat");
		assertThat(JsonPath.<String>read(rec, "$.data.products[0].name")).isEqualTo("Ayam Goreng");
		assertThat(num(rec, "$.data.products[0].predictedQty")).isEqualTo(13);
		assertThat(num(rec, "$.data.products[0].lowerQty")).isEqualTo(10);
		assertThat(num(rec, "$.data.products[0].upperQty")).isEqualTo(16);

		// Cashier boleh melihat saran, tetapi tanpa angka biaya
		String forCashier = json(as(get("/api/v1/recommendations?date=2026-11-03"), s.cashier(), ""));
		assertThat(num(forCashier, "$.data.items[0].recommendedQty")).isEqualTo(14);
		assertThat(JsonPath.<Object>read(forCashier, "$.data.items[0].estimatedCost")).isNull();
		mvc.perform(as(get("/api/v1/recommendations?date=2026-11-10"), s.cashier(), ""))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));

		// saran disalin ke baris stok saat sesi dibuka (bahan pembanding akurasi)
		clock.set("2026-11-03T04:00:00Z");
		String day2 = json(as(post("/api/v1/sessions/open"), s.owner(), ""));
		List<Object> recommended = JsonPath.read(day2, "$.data.stock[?(@.name=='Ayam')].recommendedQty");
		assertThat(((Number) recommended.get(0)).doubleValue()).isEqualTo(14.0);

		// hari kedua memperluas riwayat: permintaan 13 (dikoreksi) dan 6 -> rata-rata 9,5 dibulatkan 10
		String sid2 = JsonPath.read(day2, "$.data.id");
		mvc.perform(as(post("/api/v1/sessions/" + sid2 + "/stock"), s.owner(), "{\"items\":[{\"stockItemId\":\""
				+ s.ayam() + "\",\"quantity\":10,\"totalCost\":70000}]}")).andExpect(status().isOk());
		clock.set("2026-11-03T06:00:00Z");
		fixture.sell(s.cashier(), sid2, s.goreng(), 6);
		clock.set("2026-11-03T14:00:00Z");
		fixture.close(s.owner(), sid2, s, "4", "400", "0"); // sisa: ayam 4, tepung 400 (dari 1.000 kemarin - 600)

		clock.set("2026-11-04T01:00:00Z");
		String rec2 = json(as(get("/api/v1/recommendations"), s.owner(), ""));
		assertThat(JsonPath.<String>read(rec2, "$.data.targetDate")).isEqualTo("2026-11-04");
		assertThat(num(rec2, "$.data.dataPoints")).isEqualTo(2);
		assertThat(num(rec2, "$.data.products[0].predictedQty")).isEqualTo(10);
		assertThat(num(rec2, "$.data.products[0].lowerQty")).isEqualTo(7);
		assertThat(num(rec2, "$.data.products[0].upperQty")).isEqualTo(12);
		assertThat(num(rec2, "$.data.items[0].requiredQty")).isEqualTo(10);
		assertThat(num(rec2, "$.data.items[0].carryOverQty")).isEqualTo(4); // sisa ayam dikurangkan
		assertThat(num(rec2, "$.data.items[0].recommendedQty")).isEqualTo(7); // 10 x 1,05 - 4 = 6,5
		assertThat(num(rec2, "$.data.items[0].lowerQty")).isEqualTo(4); // 7 x 1,05 - 4 = 3,35
		assertThat(num(rec2, "$.data.items[0].upperQty")).isEqualTo(9); // 12 x 1,05 - 4 = 8,6
		assertThat(JsonPath.<List<String>>read(rec2, "$.data.items[0].reasons[*].code"))
				.containsExactly("CARRY_OVER", "LOW_DATA");
		// saran tanggal 3 tidak ikut berubah
		assertThat(num(json(as(get("/api/v1/recommendations?date=2026-11-03"), s.owner(), "")),
				"$.data.items[0].recommendedQty")).isEqualTo(14);
	}

	@Test
	void emptyDayStillClosesWithZeroSummaryAndNoRecommendation() throws Exception {
		Shop s = fixture.create();
		String sid = fixture.closedDay(s, clock, 0);
		String summary = json(as(get("/api/v1/sessions/" + sid + "/summary"), s.owner(), ""));
		assertThat(num(summary, "$.data.revenue")).isZero();
		assertThat(num(summary, "$.data.totalItemsSold")).isZero();
		assertThat(num(summary, "$.data.voidCount")).isZero();
		assertThat(JsonPath.<Object>read(summary, "$.data.nextDayRecommendationDate")).isNull();
		clock.set("2026-11-03T01:00:00Z");
		mvc.perform(as(get("/api/v1/recommendations"), s.owner(), "")).andExpect(status().isNotFound());
	}

	@Test
	void autoClosedSessionGetsSummaryAndCarryOverReducesRecommendation() throws Exception {
		Shop s = fixture.create();
		clock.set("2026-11-02T04:00:00Z");
		String sid = fixture.open(s.owner(), s);
		clock.set("2026-11-02T05:00:00Z");
		fixture.sell(s.cashier(), sid, s.goreng(), 4);
		clock.set("2026-11-02T14:30:00Z"); // batas tutup otomatis
		sessionService.autoCloseDue();

		String summary = json(as(get("/api/v1/sessions/" + sid + "/summary"), s.owner(), ""));
		assertThat(JsonPath.<String>read(summary, "$.data.closeType")).isEqualTo("AUTO");
		assertThat(num(summary, "$.data.revenue")).isEqualTo(52000);
		assertThat(num(summary, "$.data.wasteCost")).isZero(); // sambal stok dari rumah, nilainya 0
		assertThat(JsonPath.<Object>read(summary, "$.data.primaryItem.soldOutAt")).isNull();

		// sisa ayam 6 dibawa dan masih layak; kebutuhan 4 x 1,05 = 4,2 sudah tertutup sisa
		clock.set("2026-11-03T01:00:00Z");
		String rec = json(as(get("/api/v1/recommendations"), s.owner(), ""));
		assertThat(num(rec, "$.data.items[0].requiredQty")).isEqualTo(4);
		assertThat(num(rec, "$.data.items[0].carryOverQty")).isEqualTo(6);
		assertThat(num(rec, "$.data.items[0].recommendedQty")).isZero();
		assertThat(JsonPath.<List<String>>read(rec, "$.data.items[0].reasons[*].code")).contains("CARRY_OVER");
	}

	@Test
	void summaryAndRecommendationsAreOwnerOnlyAndIsolated() throws Exception {
		Shop a = fixture.create();
		Shop b = fixture.create();
		String sid = fixture.closedDay(a, clock, 3);

		mvc.perform(as(get("/api/v1/sessions/" + sid + "/summary"), a.cashier(), ""))
				.andExpect(status().isForbidden());
		mvc.perform(as(get("/api/v1/sessions/" + sid + "/summary"), b.owner(), ""))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));
		mvc.perform(as(get("/api/v1/sessions/" + UUID.randomUUID() + "/summary"), a.owner(), ""))
				.andExpect(status().isNotFound());
		mvc.perform(as(get("/api/v1/recommendations?date=2026-11-03"), a.owner(), "")).andExpect(status().isOk());
		mvc.perform(as(get("/api/v1/recommendations?date=2026-11-03"), b.owner(), ""))
				.andExpect(status().isNotFound());
	}

}
