package com.thefarhany.stockwise;

import static com.thefarhany.stockwise.ShopFixture.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.thefarhany.stockwise.ShopFixture.Shop;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Tanpa @Transactional: tiap request commit/rollback sungguhan. Tiap tes memakai usaha sendiri. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ReportIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestClock clock;

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

	private void sellQris(Shop s, String sessionId, String product, int qty) throws Exception {
		mvc.perform(as(post("/api/v1/sales"), s.cashier(), "{\"sessionId\":\"" + sessionId
				+ "\",\"paymentMethod\":\"QRIS\",\"items\":[{\"productId\":\"" + product + "\",\"quantity\":" + qty
				+ "}]}")).andExpect(status().isCreated());
	}

	// --- laporan harian ---

	@Test
	void dailyReportBreaksDownTheDay() throws Exception {
		Shop s = fixture.create();
		clock.set("2026-11-02T04:00:00Z"); // 11:00 WIB
		String sid = fixture.open(s.owner(), s);
		clock.set("2026-11-02T05:10:00Z"); // 12:10 WIB
		fixture.sell(s.cashier(), sid, s.goreng(), 2); // tunai 26.000
		clock.set("2026-11-02T07:30:00Z"); // 14:30 WIB
		sellQris(s, sid, s.paket(), 1); // QRIS 20.000
		clock.set("2026-11-02T07:45:00Z");
		String mistake = fixture.sell(s.cashier(), sid, s.goreng(), 1);
		mvc.perform(as(post("/api/v1/sales/" + mistake + "/void"), s.cashier(), "{\"reason\":\"Salah tekan\"}"))
				.andExpect(status().isOk());
		clock.set("2026-11-02T14:00:00Z");
		fixture.close(s.owner(), sid, s, "6", "1800", "4"); // ayam 10-4, tepung 2000-200, sambal 5-1

		String r = json(as(get("/api/v1/reports/daily?date=2026-11-02"), s.owner(), ""));
		assertThat(JsonPath.<String>read(r, "$.data.date")).isEqualTo("2026-11-02");
		assertThat(num(r, "$.data.revenue")).isEqualTo(46000); // transaksi yang di-void tidak dihitung
		assertThat(num(r, "$.data.cogs")).isEqualTo(29600); // ayam 4 x 7.000 + tepung 200 x 8
		assertThat(num(r, "$.data.wasteCost")).isZero();
		assertThat(num(r, "$.data.adjustmentCost")).isZero();
		assertThat(num(r, "$.data.carriedValue")).isEqualTo(56400); // ayam 6 x 7.000 + tepung 1.800 x 8
		assertThat(num(r, "$.data.expenseTotal")).isZero();
		assertThat(num(r, "$.data.netProfit")).isEqualTo(16400);
		assertThat(num(r, "$.data.transactions")).isEqualTo(2);
		assertThat(num(r, "$.data.voidCount")).isEqualTo(1);

		assertThat(JsonPath.<List<String>>read(r, "$.data.byProduct[*].name")).containsExactly("Ayam Goreng", "Paket");
		assertThat(num(r, "$.data.byProduct[0].qty")).isEqualTo(2);
		assertThat(num(r, "$.data.byProduct[0].revenue")).isEqualTo(26000);
		assertThat(num(r, "$.data.byProduct[1].revenue")).isEqualTo(20000);
		// jam menurut zona waktu usaha (WIB), hanya jam yang ada penjualan selesai
		assertThat(JsonPath.<List<Integer>>read(r, "$.data.byHour[*].hour")).containsExactly(12, 14);
		assertThat(num(r, "$.data.byHour[0].qty")).isEqualTo(2);
		assertThat(num(r, "$.data.byHour[1].qty")).isEqualTo(1);
		Map<String, Object> payments = JsonPath.read(r, "$.data.byPaymentMethod");
		assertThat(payments).hasSize(2);
		assertThat(((Number) payments.get("CASH")).longValue()).isEqualTo(26000);
		assertThat(((Number) payments.get("QRIS")).longValue()).isEqualTo(20000);
		// laba laporan = laba ringkasan (satu sumber angka)
		String summary = json(as(get("/api/v1/sessions/" + sid + "/summary"), s.owner(), ""));
		assertThat(num(summary, "$.data.netProfit")).isEqualTo(num(r, "$.data.netProfit"));
	}

	@Test
	void dailyReportRulesForMissingOpenAndForbidden() throws Exception {
		Shop a = fixture.create();
		Shop b = fixture.create();
		fixture.closedDay(a, clock, 2);
		mvc.perform(as(get("/api/v1/reports/daily?date=2026-10-01"), a.owner(), ""))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
		mvc.perform(as(get("/api/v1/reports/daily"), a.owner(), "")).andExpect(status().isBadRequest());
		mvc.perform(as(get("/api/v1/reports/daily?date=bukan-tanggal"), a.owner(), ""))
				.andExpect(status().isBadRequest());
		mvc.perform(as(get("/api/v1/reports/daily?date=2026-11-02"), a.cashier(), ""))
				.andExpect(status().isForbidden());
		mvc.perform(as(get("/api/v1/reports/daily?date=2026-11-02"), b.owner(), ""))
				.andExpect(status().isNotFound()); // data usaha lain tidak terlihat

		// sesi yang masih terbuka belum punya laporan
		clock.set("2026-11-03T04:00:00Z");
		fixture.open(a.owner(), a);
		mvc.perform(as(get("/api/v1/reports/daily?date=2026-11-03"), a.owner(), ""))
				.andExpect(status().isNotFound());
	}

	// --- riwayat sesi ---

	@Test
	void sessionHistoryListsNewestFirstWithFiguresAndFilters() throws Exception {
		Shop s = fixture.create();
		fixture.closedDay(s, clock, 3); // 2 Nov: terjual 3
		clock.set("2026-11-03T04:00:00Z");
		String day2 = JsonPath.read(json(as(post("/api/v1/sessions/open"), s.owner(), "")), "$.data.id");
		clock.set("2026-11-03T14:00:00Z");
		fixture.close(s.owner(), day2, s, "7", "1700", "0"); // tanpa penjualan
		clock.set("2026-11-04T04:00:00Z");
		String day3 = JsonPath.read(json(as(post("/api/v1/sessions/open"), s.owner(), "")), "$.data.id");

		String all = json(as(get("/api/v1/sessions?from=2026-11-01&to=2026-11-30"), s.owner(), ""));
		assertThat(num(all, "$.totalElements")).isEqualTo(3);
		assertThat(JsonPath.<List<String>>read(all, "$.data[*].businessDate"))
				.containsExactly("2026-11-04", "2026-11-03", "2026-11-02");
		assertThat(JsonPath.<String>read(all, "$.data[0].id")).isEqualTo(day3);
		// sesi terbuka: belum ada angka
		assertThat(JsonPath.<String>read(all, "$.data[0].status")).isEqualTo("OPEN");
		assertThat(JsonPath.<Object>read(all, "$.data[0].revenue")).isNull();
		assertThat(JsonPath.<Object>read(all, "$.data[0].closeType")).isNull();
		// hari tanpa penjualan
		assertThat(JsonPath.<String>read(all, "$.data[1].status")).isEqualTo("CLOSED");
		assertThat(num(all, "$.data[1].revenue")).isZero();
		// hari dengan penjualan: 3 x 13.000; biaya ayam 3 x 7.000 + tepung 300 x 8
		assertThat(JsonPath.<String>read(all, "$.data[2].closeType")).isEqualTo("MANUAL");
		assertThat(num(all, "$.data[2].revenue")).isEqualTo(39000);
		assertThat(num(all, "$.data[2].cogs")).isEqualTo(23400);
		assertThat(num(all, "$.data[2].wasteCost")).isZero();
		assertThat(num(all, "$.data[2].netProfit")).isEqualTo(15600);
		assertThat(JsonPath.<String>read(all, "$.data[2].primaryItem.name")).isEqualTo("Ayam");
		assertThat(num(all, "$.data[2].primaryItem.soldQty")).isEqualTo(3);
		assertThat(JsonPath.<Object>read(all, "$.data[2].primaryItem.soldOutAt")).isNull();
		// saran yang dulu diberikan untuk 3 Nov: kebutuhan 3 x 1,05 sudah tertutup sisa ayam 7 -> 0
		assertThat(num(all, "$.data[1].primaryItem.recommendedQty")).isZero();

		// filter rentang dan halaman
		assertThat(num(json(as(get("/api/v1/sessions?from=2026-11-03&to=2026-11-03"), s.owner(), "")),
				"$.totalElements")).isEqualTo(1);
		String page2 = json(as(get("/api/v1/sessions?from=2026-11-01&to=2026-11-30&size=2&page=1"), s.owner(), ""));
		assertThat(JsonPath.<List<Object>>read(page2, "$.data")).hasSize(1);
		assertThat(num(page2, "$.totalPages")).isEqualTo(2);
		assertThat(num(page2, "$.page")).isEqualTo(1);

		// tanpa rentang: 31 hari terakhir sampai hari ini menurut zona waktu usaha
		clock.set("2026-11-04T08:00:00Z");
		assertThat(num(json(as(get("/api/v1/sessions"), s.owner(), "")), "$.totalElements")).isEqualTo(3);
		mvc.perform(as(get("/api/v1/sessions?from=2026-11-30&to=2026-11-01"), s.owner(), ""))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.details.fields.from").exists());
	}

	@Test
	void sessionHistoryIsOwnerOnlyAndIsolated() throws Exception {
		Shop a = fixture.create();
		Shop b = fixture.create();
		fixture.closedDay(a, clock, 1);

		mvc.perform(as(get("/api/v1/sessions?from=2026-11-01&to=2026-11-30"), a.cashier(), ""))
				.andExpect(status().isForbidden());
		mvc.perform(as(get("/api/v1/sessions?from=2026-11-01&to=2026-11-30"), b.owner(), ""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
		// rute lama tetap jalan berdampingan
		mvc.perform(as(get("/api/v1/sessions/current"), a.cashier(), "")).andExpect(status().isOk());
	}

}
