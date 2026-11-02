package com.thefarhany.stockwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.thefarhany.stockwise.service.session.SessionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
/** Tanpa @Transactional: tiap request commit/rollback sungguhan, seperti di produksi. Tiap tes memakai usaha sendiri. */
class SessionIntegrationTest {

	// Jam usaha 11:00-21:00 WIB (UTC+7): buka 04:00Z, tutup 14:00Z, batas tutup otomatis 14:30Z.
	private static final String DAY1_OPEN = "2026-11-02T04:00:00Z";
	private static final String DAY1_CLOSE = "2026-11-02T14:00:00Z";
	private static final String DAY2_OPEN = "2026-11-03T04:00:00Z";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	TestClock clock;

	@Autowired
	SessionService sessionService;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	/** Usaha lengkap: Ayam (PCS, utama), Tepung (GRAM), Sambal (PCS, tidak boleh dibawa); produk Ayam Goreng. */
	private record Shop(String owner, String cashier, String ayam, String tepung, String sambal, String product) {
	}

	private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, String token, String body) {
		return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private String json(MockHttpServletRequestBuilder b) throws Exception {
		return mvc.perform(b).andReturn().getResponse().getContentAsString();
	}

	private String id(String token, String path, String body) throws Exception {
		return JsonPath.read(json(as(post(path), token, body)), "$.data.id");
	}

	private String ownerOnly() throws Exception {
		String email = "s-" + java.util.UUID.randomUUID() + "@x.id";
		String reg = json(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(
				"{\"name\":\"Budi\",\"email\":\"" + email + "\",\"password\":\"rahasia123\",\"outletName\":\"U\"}"));
		return JsonPath.read(reg, "$.data.accessToken");
	}

	private Shop shop() throws Exception {
		String owner = ownerOnly();
		mvc.perform(as(put("/api/v1/outlet"), owner,
				"{\"name\":\"U\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\",\"paydayDays\":[25,1],"
						+ "\"recommendationBufferPct\":5}")).andExpect(status().isOk());
		String ayam = id(owner, "/api/v1/stock-items",
				"{\"name\":\"Ayam\",\"kind\":\"RAW\",\"unit\":\"PCS\",\"unitCost\":7000,\"isPrimary\":true}");
		String tepung = id(owner, "/api/v1/stock-items", "{\"name\":\"Tepung\",\"kind\":\"RAW\",\"unit\":\"GRAM\"}");
		String sambal = id(owner, "/api/v1/stock-items",
				"{\"name\":\"Sambal\",\"kind\":\"RAW\",\"unit\":\"PCS\",\"carryOverAllowed\":false}");
		String product = id(owner, "/api/v1/products", "{\"name\":\"Ayam Goreng\",\"price\":13000}");
		mvc.perform(as(put("/api/v1/products/" + product + "/recipe"), owner, "{\"items\":[{\"stockItemId\":\""
				+ ayam + "\",\"quantity\":1},{\"stockItemId\":\"" + tepung + "\",\"quantity\":100}]}"))
				.andExpect(status().isOk());
		String email = "k-" + java.util.UUID.randomUUID() + "@x.id";
		mvc.perform(as(post("/api/v1/users"), owner,
				"{\"name\":\"Siti\",\"email\":\"" + email + "\",\"password\":\"kasir12345\"}"))
				.andExpect(status().isCreated());
		String cashier = JsonPath.read(json(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"kasir12345\"}")), "$.data.accessToken");
		return new Shop(owner, cashier, ayam, tepung, sambal, product);
	}

	private static String line(String item, String qty, String cost) {
		return "{\"stockItemId\":\"" + item + "\",\"quantity\":" + qty + (cost.isEmpty() ? "" : "," + cost) + "}";
	}

	private static String lines(String... l) {
		return String.join(",", l);
	}

	private static String open(String... l) {
		return "{\"stock\":[" + lines(l) + "]}";
	}

	/** Buka dengan belanja lengkap: ayam 100 @700.000, tepung 15 KG @120.000, sambal 10 tanpa biaya. */
	private String openFull(Shop s, String token) throws Exception {
		return json(as(post("/api/v1/sessions/open"), token, open(
				line(s.ayam, "100", "\"totalCost\":700000"),
				line(s.tepung, "15", "\"unit\":\"KG\",\"totalCost\":120000"),
				line(s.sambal, "10", "\"noCost\":true"))));
	}

	private static Object field(String sessionJson, String name, String field) {
		List<Object> found = JsonPath.read(sessionJson, "$.data.stock[?(@.name=='" + name + "')]." + field);
		return found.get(0);
	}

	private static double num(String sessionJson, String name, String field) {
		return ((Number) field(sessionJson, name, field)).doubleValue();
	}

	/** Meniru penjualan (modul sales belum ada): isi sold_qty dan catat movement SALE agar buku besar tetap seimbang. */
	private void setSold(String sessionId, String itemId, String qty) {
		jdbc.update("UPDATE session_stock SET sold_qty = ? WHERE session_id = UUID_TO_BIN(?) "
				+ "AND stock_item_id = UUID_TO_BIN(?)", qty, sessionId, itemId);
		jdbc.update("INSERT INTO stock_movement (session_id, stock_item_id, type, quantity) "
				+ "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), 'SALE', ?)", sessionId, itemId, "-" + qty);
	}

	private String closeBody(Shop s, String ayam, String tepung, String sambal) {
		return "{\"items\":[" + lines(ayam, tepung, sambal) + "],\"notes\":\"hujan\"}";
	}

	private static String count(String item, String closing, String carry, String discard) {
		return "{\"stockItemId\":\"" + item + "\",\"closingQty\":" + closing + ",\"carryOverQty\":" + carry
				+ ",\"discardQty\":" + discard + "}";
	}

	// --- aturan buka sesi ---

	@Test
	void openNeedsCompleteSetup() throws Exception {
		String owner = ownerOnly();
		mvc.perform(as(post("/api/v1/sessions/open"), owner, ""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("SETUP_REQUIRED"))
				.andExpect(jsonPath("$.details.missing.length()").value(4));
	}

	@Test
	void openValidatesCostUnitsAndItems() throws Exception {
		Shop s = shop();
		clock.set(DAY1_OPEN);
		String url = "/api/v1/sessions/open";
		// tanpa biaya sama sekali, dua-duanya, atau nol
		mvc.perform(as(post(url), s.owner, open(line(s.ayam, "5", ""))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("COST_REQUIRED"));
		mvc.perform(as(post(url), s.owner, open(line(s.ayam, "5", "\"totalCost\":100,\"noCost\":true"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("COST_REQUIRED"));
		mvc.perform(as(post(url), s.owner, open(line(s.ayam, "5", "\"totalCost\":0"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("COST_REQUIRED"));
		// satuan beda keluarga
		mvc.perform(as(post(url), s.owner, open(line(s.tepung, "1", "\"unit\":\"LITER\",\"totalCost\":100"))))
				.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errorCode").value("UNIT_MISMATCH"));
		// dobel dan bahan tak dikenal
		mvc.perform(as(post(url), s.owner, open(line(s.ayam, "1", "\"noCost\":true"),
				line(s.ayam, "2", "\"noCost\":true"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(as(post(url), s.owner,
				open(line("00000000-0000-0000-0000-000000000009", "1", "\"noCost\":true"))))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"));
		mvc.perform(as(post(url), s.owner, open(line(s.ayam, "0", "\"noCost\":true"))))
				.andExpect(status().isBadRequest());
		// semua penolakan di atas tidak boleh meninggalkan sesi
		mvc.perform(as(get("/api/v1/sessions/current"), s.owner, ""))
				.andExpect(jsonPath("$.data").value((Object) null));
	}

	@Test
	void openWithPurchasesConvertsUnitsAndBlocksSecondSession() throws Exception {
		Shop s = shop();
		clock.set(DAY1_OPEN);
		String opened = json(as(post("/api/v1/sessions/open"), s.cashier, open(
				line(s.ayam, "100", "\"totalCost\":700000"),
				line(s.tepung, "15", "\"unit\":\"KG\",\"totalCost\":120000"),
				line(s.sambal, "10", "\"noCost\":true"))));
		assertThat(JsonPath.<String>read(opened, "$.data.status")).isEqualTo("OPEN");
		assertThat(JsonPath.<String>read(opened, "$.data.businessDate")).isEqualTo("2026-11-02");
		// yang tersimpan di database harus sama dengan yang dilaporkan (tidak ada pergeseran zona waktu)
		assertThat(jdbc.queryForObject("SELECT CAST(business_date AS CHAR) FROM daily_session WHERE id = UUID_TO_BIN(?)",
				String.class, (String) JsonPath.read(opened, "$.data.id"))).isEqualTo("2026-11-02");
		assertThat(jdbc.queryForObject("SELECT CAST(planned_close_time AS CHAR) FROM daily_session WHERE id = "
				+ "UUID_TO_BIN(?)", String.class, (String) JsonPath.read(opened, "$.data.id"))).isEqualTo("21:00:00");
		assertThat(JsonPath.<String>read(opened, "$.data.autoCloseAt")).isEqualTo("2026-11-02T14:30:00Z");
		assertThat(num(opened, "Ayam", "availableQty")).isEqualTo(100.0);
		assertThat(num(opened, "Tepung", "availableQty")).isEqualTo(15000.0); // 15 KG -> gram
		assertThat(num(opened, "Sambal", "addedQty")).isEqualTo(10.0);

		mvc.perform(as(post("/api/v1/sessions/open"), s.owner, ""))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_ALREADY_OPEN"))
				.andExpect(jsonPath("$.details.openSessionId").exists());
		mvc.perform(as(get("/api/v1/sessions/current"), s.cashier, ""))
				.andExpect(jsonPath("$.data.stock.length()").value(3));
		// movement pembelian tercatat dengan harga per satuan
		Integer purchases = jdbc.queryForObject(
				"SELECT COUNT(*) FROM stock_movement WHERE type = 'PURCHASE'", Integer.class);
		assertThat(purchases).isGreaterThanOrEqualTo(3);
	}

	@Test
	void openTimeRulesAndBusinessDateFollowOutletZone() throws Exception {
		Shop s = shop();
		// 01:00 WIB tanggal 2 masih belum jam buka, tapi boleh dibuka; tanggal dagang = 2 (bukan 1 menurut UTC)
		clock.set("2026-11-01T18:00:00Z");
		mvc.perform(as(post("/api/v1/sessions/open"), s.owner, ""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.businessDate").value("2026-11-02"));
	}

	@Test
	void openRejectedFromAutoCloseCutoff() throws Exception {
		Shop s = shop();
		clock.set("2026-11-02T14:30:00Z");
		mvc.perform(as(post("/api/v1/sessions/open"), s.owner, ""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.errorCode").value("SESSION_CUTOFF_PASSED"));
		clock.set("2026-11-02T14:29:59Z");
		mvc.perform(as(post("/api/v1/sessions/open"), s.owner, "")).andExpect(status().isCreated());
	}

	// --- tambah stok ---

	@Test
	void addStockMidDayAndClearSoldOut() throws Exception {
		Shop s = shop();
		clock.set(DAY1_OPEN);
		String opened = openFull(s, s.owner);
		String sid = JsonPath.read(opened, "$.data.id");
		jdbc.update("UPDATE session_stock SET sold_qty = 100, sold_out_at = NOW(6) WHERE session_id = UUID_TO_BIN(?) "
				+ "AND stock_item_id = UUID_TO_BIN(?)", sid, s.ayam);

		String after = json(as(post("/api/v1/sessions/" + sid + "/stock"), s.cashier,
				"{\"items\":[" + line(s.ayam, "20", "\"totalCost\":160000") + "]}"));
		assertThat(num(after, "Ayam", "addedQty")).isEqualTo(120.0);
		assertThat(num(after, "Ayam", "availableQty")).isEqualTo(20.0);
		assertThat(field(after, "Ayam", "soldOutAt")).isNull();

		mvc.perform(as(post("/api/v1/sessions/" + sid + "/stock"), s.owner, "{\"items\":[]}"))
				.andExpect(status().isBadRequest());
		mvc.perform(as(post("/api/v1/sessions/" + sid + "/stock"), s.owner,
				"{\"items\":[" + line(s.ayam, "1", "") + "]}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COST_REQUIRED"));
	}

	// --- tutup manual, HPP, sisa ke besok ---

	@Test
	void manualCloseComputesCostsBalancesAndCarriesToNextDay() throws Exception {
		Shop s = shop();
		clock.set(DAY1_OPEN);
		String sid = JsonPath.read(openFull(s, s.owner), "$.data.id");
		json(as(post("/api/v1/sessions/" + sid + "/stock"), s.owner,
				"{\"items\":[" + line(s.ayam, "20", "\"totalCost\":160000") + "]}")); // ayam 120 @ 860.000
		setSold(sid, s.ayam, "100");
		setSold(sid, s.tepung, "3000");

		clock.set(DAY1_CLOSE);
		// ayam tercatat 20, dihitung 15: bawa 10, buang 5; tepung bawa semua; sambal buang semua
		mvc.perform(as(post("/api/v1/sessions/" + sid + "/close"), s.owner, closeBody(s,
				count(s.ayam, "15", "10", "5"), count(s.tepung, "12000", "12000", "0"),
				count(s.sambal, "10", "0", "10"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CLOSED"))
				.andExpect(jsonPath("$.data.closeType").value("MANUAL"));

		String detail = json(as(get("/api/v1/sessions/" + sid), s.owner, ""));
		// rata-rata ayam 860.000 / 120 = 7.166,667
		assertThat(num(detail, "Ayam", "soldCost")).isEqualTo(716667.0);
		assertThat(num(detail, "Ayam", "discardedCost")).isEqualTo(35833.0);
		assertThat(num(detail, "Ayam", "adjustmentCost")).isEqualTo(35833.0);
		assertThat(num(detail, "Ayam", "carriedValue")).isEqualTo(71667.0);
		assertThat(num(detail, "Ayam", "adjustedQty")).isEqualTo(-5.0);
		assertThat(num(detail, "Tepung", "soldCost")).isEqualTo(24000.0); // 3.000 g x 8
		assertThat(num(detail, "Tepung", "carriedValue")).isEqualTo(96000.0);
		assertThat(num(detail, "Sambal", "discardedCost")).isZero(); // stok dari rumah

		// persamaan nilai pas untuk semua bahan, dan semua stok masuk akhirnya keluar
		Long gap = jdbc.queryForObject("SELECT SUM(opening_value + added_cost) - SUM(sold_cost + discarded_cost "
				+ "+ adjustment_cost + carried_value) FROM session_stock WHERE session_id = UUID_TO_BIN(?)",
				Long.class, sid);
		assertThat(gap).isZero();
		assertThat(jdbc.queryForList("SELECT stock_item_id FROM stock_movement WHERE session_id = UUID_TO_BIN(?) "
				+ "GROUP BY stock_item_id HAVING SUM(quantity) <> 0", sid)).isEmpty();

		// final: tidak bisa tutup lagi, tambah stok, atau buka lagi hari yang sama
		mvc.perform(as(post("/api/v1/sessions/" + sid + "/close"), s.owner, closeBody(s,
				count(s.ayam, "15", "10", "5"), count(s.tepung, "12000", "12000", "0"),
				count(s.sambal, "10", "0", "10"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_OPEN"));
		mvc.perform(as(post("/api/v1/sessions/" + sid + "/stock"), s.owner,
				"{\"items\":[" + line(s.ayam, "1", "\"noCost\":true") + "]}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_OPEN"));
		clock.set("2026-11-02T14:10:00Z");
		mvc.perform(as(post("/api/v1/sessions/open"), s.owner, ""))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_DATE_EXISTS"));

		// hari berikutnya: sisa dan nilainya jadi stok awal, umur naik
		clock.set(DAY2_OPEN);
		String day2 = json(as(post("/api/v1/sessions/open"), s.owner,
				open(line(s.ayam, "10", "\"totalCost\":80000"))));
		assertThat(num(day2, "Ayam", "openingQty")).isEqualTo(10.0);
		assertThat(num(day2, "Ayam", "openingAgeDays")).isEqualTo(1.0);
		assertThat(num(day2, "Ayam", "availableQty")).isEqualTo(20.0);
		assertThat(num(day2, "Tepung", "openingQty")).isEqualTo(12000.0);
		assertThat(num(day2, "Sambal", "openingQty")).isZero();

		// HPP produk kini dari harga rata-rata belanja: ayam (71.667 + 80.000) / 20 = 7.583,35; tepung 8/g
		String products = json(as(get("/api/v1/products"), s.owner, ""));
		assertThat(JsonPath.<Integer>read(products, "$.data[0].hpp")).isEqualTo(8383); // 7.583,35 + 800
		assertThat(JsonPath.<Boolean>read(products, "$.data[0].hppEstimated")).isFalse();
		assertThat(JsonPath.<Integer>read(products, "$.data[0].maxPortionsFromStock")).isEqualTo(20);
		assertThat(JsonPath.<Boolean>read(products, "$.data[0].available")).isTrue();
		String forCashier = json(as(get("/api/v1/products"), s.cashier, ""));
		assertThat(JsonPath.<Object>read(forCashier, "$.data[0].hpp")).isNull();
		assertThat(JsonPath.<Integer>read(forCashier, "$.data[0].maxPortionsFromStock")).isEqualTo(20);
	}

	@Test
	void closeValidationRules() throws Exception {
		Shop s = shop();
		clock.set(DAY1_OPEN);
		String sid = JsonPath.read(openFull(s, s.owner), "$.data.id");
		String url = "/api/v1/sessions/" + sid + "/close";
		String ok1 = count(s.ayam, "100", "100", "0");
		String ok2 = count(s.tepung, "15000", "15000", "0");
		String ok3 = count(s.sambal, "10", "0", "10");

		mvc.perform(as(post(url), s.owner, closeBody(s, count(s.ayam, "100", "60", "30"), ok2, ok3)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(as(post(url), s.owner, "{\"items\":[" + lines(ok1, ok2) + "]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.items").exists());
		mvc.perform(as(post(url), s.owner, "{\"items\":[" + lines(ok1, ok1, ok2, ok3) + "]}"))
				.andExpect(status().isBadRequest());
		mvc.perform(as(post(url), s.owner, closeBody(s, ok1, ok2, count(s.sambal, "10", "10", "0"))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.errorCode").value("CARRY_OVER_NOT_ALLOWED"));
		mvc.perform(as(post(url), s.owner, closeBody(s, ok1, ok2,
				count("00000000-0000-0000-0000-000000000009", "1", "1", "0"))))
				.andExpect(status().isNotFound());
		mvc.perform(as(post(url), s.owner, closeBody(s, count(s.ayam, "-1", "0", "0"), ok2, ok3)))
				.andExpect(status().isBadRequest());
		// semua penolakan tadi tidak mengubah apa pun: sesi masih terbuka
		mvc.perform(as(get("/api/v1/sessions/current"), s.owner, ""))
				.andExpect(jsonPath("$.data.status").value("OPEN"))
				.andExpect(jsonPath("$.data.stock[0].closingQty").value((Object) null));
	}

	// --- tutup otomatis ---

	@Test
	void autoCloseUsesRecordedStockAtCutoffAndIsIdempotent() throws Exception {
		Shop s = shop();
		clock.set(DAY1_OPEN);
		String sid = JsonPath.read(openFull(s, s.owner), "$.data.id");
		setSold(sid, s.ayam, "40");

		clock.set("2026-11-02T14:29:00Z");
		sessionService.autoCloseDue();
		mvc.perform(as(get("/api/v1/sessions/" + sid), s.owner, "")).andExpect(jsonPath("$.data.status").value("OPEN"));

		clock.set("2026-11-02T14:30:00Z");
		sessionService.autoCloseDue();
		String detail = json(as(get("/api/v1/sessions/" + sid), s.owner, ""));
		assertThat(JsonPath.<String>read(detail, "$.data.status")).isEqualTo("CLOSED");
		assertThat(JsonPath.<String>read(detail, "$.data.closeType")).isEqualTo("AUTO");
		assertThat(num(detail, "Ayam", "carriedOverQty")).isEqualTo(60.0); // boleh dibawa
		assertThat(num(detail, "Ayam", "discardedQty")).isZero();
		assertThat(num(detail, "Sambal", "carriedOverQty")).isZero(); // tidak boleh dibawa -> terbuang
		assertThat(num(detail, "Sambal", "discardedQty")).isEqualTo(10.0);
		assertThat(num(detail, "Ayam", "soldCost")).isEqualTo(280000.0); // 40 x 7.000
		assertThat(num(detail, "Ayam", "carriedValue")).isEqualTo(420000.0);
		assertThat(num(detail, "Ayam", "adjustmentCost")).isZero(); // tanpa selisih

		assertThat(jdbc.queryForObject("SELECT closed_by IS NULL FROM daily_session WHERE id = UUID_TO_BIN(?)",
				Boolean.class, sid)).isTrue();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE session_id = UUID_TO_BIN(?) "
				+ "AND created_by IS NOT NULL AND type IN ('CARRY_OVER','DISCARD')", Integer.class, sid)).isZero();
		assertThat(sessionService.autoCloseDue()).isZero(); // dijalankan ulang: tidak ada yang berubah

		// sisa otomatis terbawa ke hari berikutnya
		clock.set(DAY2_OPEN);
		String day2 = json(as(post("/api/v1/sessions/open"), s.owner, ""));
		assertThat(num(day2, "Ayam", "openingQty")).isEqualTo(60.0);
		assertThat(num(day2, "Sambal", "openingQty")).isZero();
	}

	// --- peran dan isolasi ---

	@Test
	void cashierOperatesSessionButNeverSeesCostsOrHistory() throws Exception {
		Shop s = shop();
		clock.set(DAY1_OPEN);
		String sid = JsonPath.read(openFull(s, s.cashier), "$.data.id");
		setSold(sid, s.ayam, "10");
		clock.set(DAY1_CLOSE);
		String closed = json(as(post("/api/v1/sessions/" + sid + "/close"), s.cashier, closeBody(s,
				count(s.ayam, "90", "90", "0"), count(s.tepung, "15000", "15000", "0"),
				count(s.sambal, "10", "0", "10"))));
		assertThat(JsonPath.<String>read(closed, "$.data.status")).isEqualTo("CLOSED");
		assertThat(field(closed, "Ayam", "soldCost")).isNull();
		assertThat(field(closed, "Ayam", "carriedValue")).isNull();
		mvc.perform(as(get("/api/v1/sessions/" + sid), s.cashier, "")).andExpect(status().isForbidden());
		// Owner tetap melihat biaya dari sesi yang sama
		assertThat(num(json(as(get("/api/v1/sessions/" + sid), s.owner, "")), "Ayam", "soldCost"))
				.isEqualTo(70000.0);
	}

	@Test
	void concurrentDevicesDoNotLoseStockUpdatesOrCloseTwice() throws Exception {
		Shop s = shop();
		clock.set(DAY1_OPEN);
		String sid = JsonPath.read(openFull(s, s.owner), "$.data.id");

		// 8 perangkat menambah 10 ayam sekaligus: tidak boleh ada pembaruan yang hilang (100 + 80)
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8);
		java.util.concurrent.CountDownLatch goAdd = new java.util.concurrent.CountDownLatch(1);
		List<java.util.concurrent.Future<Integer>> adds = new java.util.ArrayList<>();
		for (int i = 0; i < 8; i++) {
			adds.add(pool.submit(() -> {
				goAdd.await();
				return mvc.perform(as(post("/api/v1/sessions/" + sid + "/stock"), s.cashier,
						"{\"items\":[" + line(s.ayam, "10", "\"totalCost\":70000") + "]}"))
						.andReturn().getResponse().getStatus();
			}));
		}
		goAdd.countDown();
		for (var f : adds) {
			assertThat(f.get()).isEqualTo(200);
		}
		String now = json(as(get("/api/v1/sessions/current"), s.owner, ""));
		assertThat(num(now, "Ayam", "addedQty")).isEqualTo(180.0);
		assertThat(jdbc.queryForObject("SELECT added_cost FROM session_stock WHERE session_id = UUID_TO_BIN(?) "
				+ "AND stock_item_id = UUID_TO_BIN(?)", Long.class, sid, s.ayam)).isEqualTo(700000L + 8 * 70000L);

		// dua perangkat menutup bersamaan: tepat satu berhasil, satunya SESSION_NOT_OPEN
		String body = closeBody(s, count(s.ayam, "180", "180", "0"), count(s.tepung, "15000", "15000", "0"),
				count(s.sambal, "10", "0", "10"));
		java.util.concurrent.CountDownLatch goClose = new java.util.concurrent.CountDownLatch(1);
		List<java.util.concurrent.Future<Integer>> closes = new java.util.ArrayList<>();
		for (int i = 0; i < 2; i++) {
			closes.add(pool.submit(() -> {
				goClose.await();
				return mvc.perform(as(post("/api/v1/sessions/" + sid + "/close"), s.owner, body))
						.andReturn().getResponse().getStatus();
			}));
		}
		goClose.countDown();
		List<Integer> codes = List.of(closes.get(0).get(), closes.get(1).get());
		pool.shutdown();
		assertThat(codes).containsExactlyInAnyOrder(200, 409);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE session_id = UUID_TO_BIN(?) "
				+ "AND type = 'DISCARD'", Integer.class, sid)).isEqualTo(1); // sambal, tercatat sekali
	}

	@Test
	void sessionsAreIsolatedBetweenOutlets() throws Exception {
		Shop a = shop();
		Shop b = shop();
		clock.set(DAY1_OPEN);
		String sid = JsonPath.read(openFull(a, a.owner), "$.data.id");

		mvc.perform(as(get("/api/v1/sessions/current"), b.owner, ""))
				.andExpect(jsonPath("$.data").value((Object) null));
		mvc.perform(as(get("/api/v1/sessions/" + sid), b.owner, ""))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));
		mvc.perform(as(post("/api/v1/sessions/" + sid + "/stock"), b.owner,
				"{\"items\":[" + line(b.ayam, "1", "\"noCost\":true") + "]}")).andExpect(status().isNotFound());
		mvc.perform(as(post("/api/v1/sessions/" + sid + "/close"), b.owner, "{\"items\":[]}"))
				.andExpect(status().isNotFound());
		// usaha B bisa buka sesinya sendiri di hari yang sama
		mvc.perform(as(post("/api/v1/sessions/open"), b.owner, "")).andExpect(status().isCreated());
		Map<String, Object> counts = jdbc.queryForMap("SELECT COUNT(*) AS n FROM daily_session WHERE status = 'OPEN' "
				+ "AND business_date = '2026-11-02'");
		assertThat(((Number) counts.get("n")).intValue()).isGreaterThanOrEqualTo(2);
	}

	@Test
	void productsShowNoAvailabilityWithoutOpenSession() throws Exception {
		Shop s = shop();
		String products = json(as(get("/api/v1/products"), s.owner, ""));
		assertThat(JsonPath.<Object>read(products, "$.data[0].available")).isNull();
		assertThat(JsonPath.<Object>read(products, "$.data[0].maxPortionsFromStock")).isNull();
		assertThat(JsonPath.<Boolean>read(products, "$.data[0].hppEstimated")).isTrue(); // belum pernah belanja
		ResultActions none = mvc.perform(as(get("/api/v1/sessions/current"), s.cashier, ""));
		none.andExpect(status().isOk()).andExpect(jsonPath("$.data").value((Object) null));
	}

}
