package com.thefarhany.stockwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class OutletIntegrationTest {

	private static final String PROFILE = """
			{"name":"Geprek Pak Budi","openTime":"11:00","closeTime":"21:00","latitude":-6.2088,"longitude":106.8456,
			 "paydayDays":[25,1,25],"recommendationBufferPct":7.5}""";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	jakarta.persistence.EntityManager em;

	private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token, String body) {
		return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private String register(String email, String outlet) throws Exception {
		return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(
				"{\"name\":\"Budi\",\"email\":\"" + email + "\",\"password\":\"rahasia123\",\"outletName\":\""
						+ outlet + "\"}")).andReturn().getResponse().getContentAsString();
	}

	@Test
	void newOutletIsIncompleteThenProfileAndCatalogMakeItReady() throws Exception {
		String reg = register("o1@x.id", "Geprek Pak Budi");
		String token = JsonPath.read(reg, "$.data.accessToken");
		String outletId = JsonPath.read(reg, "$.data.outlet.id");

		mvc.perform(auth(get("/api/v1/outlet"), token, ""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.setupCompleted").value(false))
				.andExpect(jsonPath("$.data.paydayDays[0]").value(25))
				.andExpect(jsonPath("$.data.onboarding.profileDone").value(false))
				.andExpect(jsonPath("$.data.onboarding.readyToOpen").value(false));

		mvc.perform(auth(put("/api/v1/outlet"), token, PROFILE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.setupCompleted").value(true))
				.andExpect(jsonPath("$.data.openTime").value("11:00:00"))
				.andExpect(jsonPath("$.data.paydayDays.length()").value(2))
				.andExpect(jsonPath("$.data.recommendationBufferPct").value(7.5))
				.andExpect(jsonPath("$.data.onboarding.profileDone").value(true))
				.andExpect(jsonPath("$.data.onboarding.readyToOpen").value(false));

		seedCatalog(outletId);
		mvc.perform(auth(get("/api/v1/outlet"), token, ""))
				.andExpect(jsonPath("$.data.onboarding.stockItems").value(2))
				.andExpect(jsonPath("$.data.onboarding.products").value(2))
				.andExpect(jsonPath("$.data.onboarding.recipes").value(1))
				.andExpect(jsonPath("$.data.onboarding.readyToOpen").value(true));
	}

	@Test
	void readyToOpenWhenCatalogBuiltThroughEndpoints() throws Exception {
		String token = JsonPath.read(register("o6@x.id", "Via Endpoint"), "$.data.accessToken");
		mvc.perform(auth(put("/api/v1/outlet"), token, PROFILE)).andExpect(status().isOk());

		String ayam = created(auth(post("/api/v1/stock-items"), token,
				"{\"name\":\"Ayam\",\"kind\":\"RAW\",\"unit\":\"PCS\",\"unitCost\":7000}"));
		String product = created(auth(post("/api/v1/products"), token, "{\"name\":\"Ayam Goreng\",\"price\":13000}"));
		em.flush(); // tes satu transaksi: hitungan onboarding memakai SQL langsung
		mvc.perform(auth(get("/api/v1/outlet"), token, ""))
				.andExpect(jsonPath("$.data.onboarding.stockItems").value(1))
				.andExpect(jsonPath("$.data.onboarding.products").value(1))
				.andExpect(jsonPath("$.data.onboarding.recipes").value(0))
				.andExpect(jsonPath("$.data.onboarding.readyToOpen").value(false));

		mvc.perform(auth(put("/api/v1/products/" + product + "/recipe"), token,
				"{\"items\":[{\"stockItemId\":\"" + ayam + "\",\"quantity\":1}]}")).andExpect(status().isOk());
		em.flush();
		em.clear();
		mvc.perform(auth(get("/api/v1/outlet"), token, ""))
				.andExpect(jsonPath("$.data.onboarding.recipes").value(1))
				.andExpect(jsonPath("$.data.onboarding.readyToOpen").value(true));

		// produk nonaktif tidak dihitung lagi
		mvc.perform(auth(delete("/api/v1/products/" + product), token, "")).andExpect(status().isOk());
		em.flush();
		em.clear();
		mvc.perform(auth(get("/api/v1/outlet"), token, ""))
				.andExpect(jsonPath("$.data.onboarding.products").value(0))
				.andExpect(jsonPath("$.data.onboarding.readyToOpen").value(false));
	}

	private String created(MockHttpServletRequestBuilder b) throws Exception {
		return JsonPath.read(mvc.perform(b).andReturn().getResponse().getContentAsString(), "$.data.id");
	}

	@Test
	void businessHoursAreStoredAsWallClockTimeRegardlessOfServerZone() throws Exception {
		assertThat(java.util.TimeZone.getDefault().getID()).as("JVM harus berzona UTC").isEqualTo("UTC");
		String reg = register("o7@x.id", "Sarapan");
		String token = JsonPath.read(reg, "$.data.accessToken");
		String outletId = JsonPath.read(reg, "$.data.outlet.id");

		// buka pagi: 06:00 WIB akan tersimpan 23:00 (hari sebelumnya) bila jam ikut digeser ke UTC
		mvc.perform(auth(put("/api/v1/outlet"), token, PROFILE.replace("11:00", "06:00").replace("21:00", "15:00")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.openTime").value("06:00:00"))
				.andExpect(jsonPath("$.data.closeTime").value("15:00:00"));
		em.flush();
		assertThat(jdbc.queryForObject("SELECT CAST(open_time AS CHAR) FROM outlet WHERE id = UUID_TO_BIN(?)",
				String.class, outletId)).isEqualTo("06:00:00");
		assertThat(jdbc.queryForObject("SELECT CAST(close_time AS CHAR) FROM outlet WHERE id = UUID_TO_BIN(?)",
				String.class, outletId)).isEqualTo("15:00:00");

		// sehari penuh melewati batas UTC
		mvc.perform(auth(put("/api/v1/outlet"), token, PROFILE.replace("11:00", "00:00").replace("21:00", "23:59")))
				.andExpect(status().isOk());
	}

	@Test
	void updateRejectsBadHoursAndBadFields() throws Exception {
		String token = JsonPath.read(register("o2@x.id", "A"), "$.data.accessToken");
		mvc.perform(auth(put("/api/v1/outlet"), token, PROFILE.replace("21:00", "09:00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.details.fields.closeTime").exists());
		mvc.perform(auth(put("/api/v1/outlet"), token, PROFILE.replace("-6.2088", "-95").replace("7.5", "150")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details.fields.latitude").exists())
				.andExpect(jsonPath("$.details.fields.recommendationBufferPct").exists());
		mvc.perform(auth(put("/api/v1/outlet"), token, PROFILE.replace("25,1,25", "0,32")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cashierCannotTouchOutlet() throws Exception {
		String token = JsonPath.read(register("o3@x.id", "A"), "$.data.accessToken");
		mvc.perform(auth(post("/api/v1/users"), token,
				"{\"name\":\"Siti\",\"email\":\"s3@x.id\",\"password\":\"kasir12345\"}")).andExpect(status().isCreated());
		String cashier = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"s3@x.id\",\"password\":\"kasir12345\"}")).andReturn().getResponse()
				.getContentAsString();
		String ct = JsonPath.read(cashier, "$.data.accessToken");
		mvc.perform(auth(get("/api/v1/outlet"), ct, "")).andExpect(status().isForbidden());
		mvc.perform(auth(put("/api/v1/outlet"), ct, PROFILE)).andExpect(status().isForbidden());
		mvc.perform(auth(delete("/api/v1/outlet"), ct, "{\"confirmName\":\"A\"}")).andExpect(status().isForbidden());
	}

	@Test
	void deleteRemovesOnlyThisOutletWithAllItsData() throws Exception {
		String reg = register("o4@x.id", "Hapus Aku");
		String token = JsonPath.read(reg, "$.data.accessToken");
		String outletId = JsonPath.read(reg, "$.data.outlet.id");
		String userId = JsonPath.read(reg, "$.data.user.id");
		String other = register("o5@x.id", "Tetap Ada");
		String otherToken = JsonPath.read(other, "$.data.accessToken");
		mvc.perform(auth(post("/api/v1/users"), token,
				"{\"name\":\"Siti\",\"email\":\"s4@x.id\",\"password\":\"kasir12345\"}")).andExpect(status().isCreated());
		seedCatalog(outletId);
		seedHistory(outletId, userId);

		mvc.perform(auth(delete("/api/v1/outlet"), token, "{\"confirmName\":\"hapus aku\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details.fields.confirmName").exists());
		mvc.perform(auth(delete("/api/v1/outlet"), token, "{\"confirmName\":\"Hapus Aku\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value((Object) null));

		for (String t : new String[] {"app_user", "stock_item", "product", "daily_session", "sale", "forecast",
				"recommendation"}) {
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + t + " WHERE outlet_id = UUID_TO_BIN(?)",
					Integer.class, outletId)).as(t).isZero();
		}
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outlet WHERE id = UUID_TO_BIN(?)", Integer.class,
				outletId)).as("outlet").isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM daily_summary WHERE session_id = "
				+ "UUID_TO_BIN('33333333-3333-3333-3333-333333333333')", Integer.class)).as("summary").isZero();
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"o4@x.id\",\"password\":\"rahasia123\"}")).andExpect(status().isUnauthorized());
		em.clear(); // tes satu transaksi: buang cache entity yang sudah dihapus lewat SQL
		mvc.perform(auth(get("/api/v1/outlet"), token, "")).andExpect(status().isUnauthorized());
		mvc.perform(auth(get("/api/v1/outlet"), otherToken, "")).andExpect(jsonPath("$.data.name").value("Tetap Ada"));
	}

	/** 2 bahan aktif, 2 produk aktif (satu beresep), 1 produk nonaktif beresep yang tidak boleh dihitung. */
	private void seedCatalog(String o) {
		em.flush(); // insert JPA harus sudah di DB sebelum SQL mentah
		jdbc.update("INSERT INTO stock_item (id, outlet_id, name, kind, unit) VALUES "
				+ "(UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), UUID_TO_BIN(?), 'Ayam', 'RAW', 'PCS'),"
				+ "(UUID_TO_BIN('11111111-1111-1111-1111-111111111112'), UUID_TO_BIN(?), 'Tepung', 'RAW', 'GRAM')", o, o);
		jdbc.update("INSERT INTO product (id, outlet_id, name, price, stock_mode, active) VALUES "
				+ "(UUID_TO_BIN('22222222-2222-2222-2222-222222222221'), UUID_TO_BIN(?), 'Ayam Goreng', 13000, 'MADE_TO_ORDER', 1),"
				+ "(UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), UUID_TO_BIN(?), 'Es Teh', 5000, 'MADE_TO_ORDER', 1),"
				+ "(UUID_TO_BIN('22222222-2222-2222-2222-222222222223'), UUID_TO_BIN(?), 'Lama', 1000, 'MADE_TO_ORDER', 0)",
				o, o, o);
		jdbc.update("INSERT INTO recipe_item VALUES "
				+ "(UUID_TO_BIN('22222222-2222-2222-2222-222222222221'), UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 1),"
				+ "(UUID_TO_BIN('22222222-2222-2222-2222-222222222223'), UUID_TO_BIN('11111111-1111-1111-1111-111111111112'), 5)");
	}

	/** Satu rantai penuh: sesi, stok, movement, penjualan, ringkasan, saran, prakiraan. */
	private void seedHistory(String o, String user) {
		em.flush();
		String sid = "33333333-3333-3333-3333-333333333333";
		String sale = "44444444-4444-4444-4444-444444444444";
		jdbc.update("INSERT INTO daily_session (id, outlet_id, business_date, status, opened_at, opened_by, planned_close_time) "
				+ "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), '2026-11-01', 'OPEN', NOW(6), UUID_TO_BIN(?), '21:00:00')",
				sid, o, user);
		jdbc.update("INSERT INTO session_stock (session_id, stock_item_id) VALUES (UUID_TO_BIN(?), "
				+ "UUID_TO_BIN('11111111-1111-1111-1111-111111111111'))", sid);
		jdbc.update("INSERT INTO stock_movement (session_id, stock_item_id, type, quantity, created_by) VALUES "
				+ "(UUID_TO_BIN(?), UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 'PURCHASE', 10, UUID_TO_BIN(?))",
				sid, user);
		jdbc.update("INSERT INTO sale (id, outlet_id, session_id, receipt_no, sold_at, total_amount, payment_method, "
				+ "input_method, cashier_id) VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), 1, NOW(6), 13000, "
				+ "'CASH', 'TAP', UUID_TO_BIN(?))", sale, o, sid, user);
		jdbc.update("INSERT INTO sale_item (sale_id, product_id, quantity, unit_price, subtotal) VALUES "
				+ "(UUID_TO_BIN(?), UUID_TO_BIN('22222222-2222-2222-2222-222222222221'), 1, 13000, 13000)", sale);
		jdbc.update("INSERT INTO daily_summary (session_id, revenue, cogs, waste_cost, adjustment_cost, carried_value, "
				+ "net_profit, total_items_sold) VALUES (UUID_TO_BIN(?), 13000, 7000, 0, 0, 0, 6000, 1)", sid);
		jdbc.update("INSERT INTO recommendation (outlet_id, target_date, stock_item_id, required_qty, recommended_qty, "
				+ "estimated_cost) VALUES (UUID_TO_BIN(?), '2026-11-02', "
				+ "UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 10, 10, 70000)", o);
		jdbc.update("INSERT INTO forecast (outlet_id, target_date, product_id, predicted_qty, lower_qty, upper_qty, "
				+ "method, model_version) VALUES (UUID_TO_BIN(?), '2026-11-02', "
				+ "UUID_TO_BIN('22222222-2222-2222-2222-222222222221'), 10, 8, 12, 'COLD_START', 'x')", o);
	}

}
