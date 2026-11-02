package com.thefarhany.stockwise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
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
class CatalogIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	EntityManager em;

	private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, String token, String body) {
		return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private String body(MockHttpServletRequestBuilder b) throws Exception {
		return mvc.perform(b).andReturn().getResponse().getContentAsString();
	}

	private String ownerToken(String email) throws Exception {
		return JsonPath.read(body(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(
				"{\"name\":\"Budi\",\"email\":\"" + email + "\",\"password\":\"rahasia123\",\"outletName\":\"U\"}")),
				"$.data.accessToken");
	}

	private String stockItem(String token, String json) throws Exception {
		return JsonPath.read(body(as(post("/api/v1/stock-items"), token, json)), "$.data.id");
	}

	private static String item(String name, String unit, long cost) {
		return "{\"name\":\"" + name + "\",\"kind\":\"RAW\",\"unit\":\"" + unit + "\",\"unitCost\":" + cost + "}";
	}

	@Test
	void stockItemCrudDefaultsDuplicatesAndValidation() throws Exception {
		String t = ownerToken("c1@x.id");
		mvc.perform(as(post("/api/v1/stock-items"), t, item("Ayam", "PCS", 7000)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.carryOverAllowed").value(true))
				.andExpect(jsonPath("$.data.shelfLifeDays").value(1))
				.andExpect(jsonPath("$.data.isPrimary").value(false))
				.andExpect(jsonPath("$.data.active").value(true));
		mvc.perform(as(post("/api/v1/stock-items"), t, item("AYAM", "PCS", 1)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("DUPLICATE_NAME"));
		mvc.perform(as(post("/api/v1/stock-items"), t, "{\"name\":\"\",\"kind\":\"X\",\"unit\":\"PCS\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(as(post("/api/v1/stock-items"), t, item("Neg", "PCS", -5)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.unitCost").exists());

		String tepung = stockItem(t, item("Tepung", "GRAM", 8));
		mvc.perform(as(put("/api/v1/stock-items/" + tepung), t,
				"{\"name\":\"ayam\",\"kind\":\"RAW\",\"unit\":\"GRAM\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("DUPLICATE_NAME"));
		mvc.perform(as(put("/api/v1/stock-items/" + tepung), t,
				"{\"name\":\"Tepung terigu\",\"kind\":\"RAW\",\"unit\":\"KG\",\"unitCost\":8000,\"isPrimary\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Tepung terigu"))
				.andExpect(jsonPath("$.data.unit").value("KG"))
				.andExpect(jsonPath("$.data.isPrimary").value(true));

		mvc.perform(as(delete("/api/v1/stock-items/" + tepung), t, "")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value((Object) null));
		mvc.perform(as(get("/api/v1/stock-items"), t, "")).andExpect(jsonPath("$.data.length()").value(1));
		mvc.perform(as(get("/api/v1/stock-items?includeInactive=true"), t, ""))
				.andExpect(jsonPath("$.data.length()").value(2));
		// aktifkan lagi lewat PUT
		mvc.perform(as(put("/api/v1/stock-items/" + tepung), t,
				"{\"name\":\"Tepung terigu\",\"kind\":\"RAW\",\"unit\":\"KG\",\"active\":true}"))
				.andExpect(jsonPath("$.data.active").value(true));
	}

	@Test
	void unitCannotChangeOnceUsedInSession() throws Exception {
		String reg = body(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(
				"{\"name\":\"B\",\"email\":\"c2@x.id\",\"password\":\"rahasia123\",\"outletName\":\"U\"}"));
		String t = JsonPath.read(reg, "$.data.accessToken");
		String outlet = JsonPath.read(reg, "$.data.outlet.id");
		String user = JsonPath.read(reg, "$.data.user.id");
		String id = stockItem(t, item("Ayam", "PCS", 7000));
		em.flush();
		jdbc.update("INSERT INTO daily_session (id, outlet_id, business_date, status, opened_at, opened_by, "
				+ "planned_close_time) VALUES (UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), UUID_TO_BIN(?), "
				+ "'2026-11-01', 'OPEN', NOW(6), UUID_TO_BIN(?), '21:00:00')", outlet, user);
		jdbc.update("INSERT INTO session_stock (session_id, stock_item_id) VALUES "
				+ "(UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), UUID_TO_BIN(?))", id);
		mvc.perform(as(put("/api/v1/stock-items/" + id), t, "{\"name\":\"Ayam\",\"kind\":\"RAW\",\"unit\":\"GRAM\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.unit").exists());
		mvc.perform(as(put("/api/v1/stock-items/" + id), t, "{\"name\":\"Ayam potong\",\"kind\":\"RAW\",\"unit\":\"PCS\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void productRecipeHppAndInUseProtection() throws Exception {
		String t = ownerToken("c3@x.id");
		String ayam = stockItem(t, item("Ayam", "PCS", 7000));
		String tepung = stockItem(t, item("Tepung", "GRAM", 8));

		String p = JsonPath.read(body(as(post("/api/v1/products"), t,
				"{\"name\":\"Ayam Goreng\",\"category\":\"Makanan\",\"price\":13000}")), "$.data.id");
		mvc.perform(as(post("/api/v1/products"), t, "{\"name\":\"ayam goreng\",\"price\":1}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("DUPLICATE_NAME"));
		mvc.perform(as(post("/api/v1/products"), t, "{\"name\":\"Batch\",\"price\":1,\"stockMode\":\"BATCH\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.stockMode").exists());

		mvc.perform(as(get("/api/v1/products"), t, ""))
				.andExpect(jsonPath("$.data[0].hpp").value((Object) null))
				.andExpect(jsonPath("$.data[0].recipe.length()").value(0));

		mvc.perform(as(put("/api/v1/products/" + p + "/recipe"), t, recipe(ayam, "1", ayam, "2")))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.items").exists());
		mvc.perform(as(put("/api/v1/products/" + p + "/recipe"), t, recipe(ayam, "0")))
				.andExpect(status().isBadRequest());
		mvc.perform(as(put("/api/v1/products/" + p + "/recipe"), t,
				recipe("00000000-0000-0000-0000-000000000009", "1")))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"));

		mvc.perform(as(put("/api/v1/products/" + p + "/recipe"), t, recipe(ayam, "1", tepung, "100")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(2));
		// ganti total: tepung jadi 120 (kunci sama dengan resep lama)
		mvc.perform(as(put("/api/v1/products/" + p + "/recipe"), t, recipe(ayam, "1", tepung, "120")))
				.andExpect(status().isOk());
		em.flush();
		em.clear();
		mvc.perform(as(get("/api/v1/products"), t, ""))
				.andExpect(jsonPath("$.data[0].hpp").value(7960)) // 7000 + 120 x 8
				.andExpect(jsonPath("$.data[0].margin").value(5040))
				.andExpect(jsonPath("$.data[0].suggestedPrice").value(13500)) // 7.960 / 0,6 = 13.266,67 -> 13.500
				.andExpect(jsonPath("$.data[0].hppEstimated").value(true))
				.andExpect(jsonPath("$.data[0].recipe.length()").value(2));

		mvc.perform(as(delete("/api/v1/stock-items/" + ayam), t, ""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_IN_USE"))
				.andExpect(jsonPath("$.details.products[0]").value("Ayam Goreng"));

		mvc.perform(as(delete("/api/v1/products/" + p), t, "")).andExpect(status().isOk());
		mvc.perform(as(get("/api/v1/products"), t, "")).andExpect(jsonPath("$.data.length()").value(0));
		mvc.perform(as(get("/api/v1/products?includeInactive=true"), t, ""))
				.andExpect(jsonPath("$.data[0].active").value(false));
		mvc.perform(as(delete("/api/v1/stock-items/" + ayam), t, "")).andExpect(status().isOk());
	}

	private static String recipe(String... pairs) {
		StringBuilder sb = new StringBuilder("{\"items\":[");
		for (int i = 0; i < pairs.length; i += 2) {
			sb.append(i > 0 ? "," : "").append("{\"stockItemId\":\"").append(pairs[i]).append("\",\"quantity\":")
					.append(pairs[i + 1]).append("}");
		}
		return sb.append("]}").toString();
	}

	@Test
	void suggestedPriceFollowsTheOutletTargetMargin() throws Exception {
		String t = ownerToken("c6@x.id");
		String ayam = stockItem(t, item("Ayam", "PCS", 7000));
		String tepung = stockItem(t, item("Tepung", "GRAM", 8));
		String p = JsonPath.read(body(as(post("/api/v1/products"), t, "{\"name\":\"AG\",\"price\":13000}")),
				"$.data.id");
		mvc.perform(as(put("/api/v1/products/" + p + "/recipe"), t, recipe(ayam, "1", tepung, "100")))
				.andExpect(status().isOk());
		em.flush();
		em.clear();
		// default 40%: HPP 7.800 -> Rp13.000 (contoh PRD)
		mvc.perform(as(get("/api/v1/outlet"), t, "")).andExpect(jsonPath("$.data.targetMarginPct").value(40.0));
		mvc.perform(as(get("/api/v1/products"), t, "")).andExpect(jsonPath("$.data[0].hpp").value(7800))
				.andExpect(jsonPath("$.data[0].suggestedPrice").value(13000));

		String profile = "{\"name\":\"U\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\",\"paydayDays\":[25,1],"
				+ "\"recommendationBufferPct\":5";
		mvc.perform(as(put("/api/v1/outlet"), t, profile + ",\"targetMarginPct\":50}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.targetMarginPct").value(50));
		em.flush();
		em.clear();
		mvc.perform(as(get("/api/v1/products"), t, ""))
				.andExpect(jsonPath("$.data[0].suggestedPrice").value(16000)); // 7.800 / 0,5 = 15.600 -> 16.000

		// tanpa targetMarginPct: nilai lama dipertahankan; di atas 90% ditolak
		mvc.perform(as(put("/api/v1/outlet"), t, profile + "}"))
				.andExpect(jsonPath("$.data.targetMarginPct").value(50));
		mvc.perform(as(put("/api/v1/outlet"), t, profile + ",\"targetMarginPct\":95}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.targetMarginPct").exists());
	}

	@Test
	void cashierReadsButCannotWriteAndSeesNoCosts() throws Exception {
		String t = ownerToken("c4@x.id");
		String ayam = stockItem(t, item("Ayam", "PCS", 7000));
		String p = JsonPath.read(body(as(post("/api/v1/products"), t, "{\"name\":\"AG\",\"price\":13000}")),
				"$.data.id");
		mvc.perform(as(put("/api/v1/products/" + p + "/recipe"), t, recipe(ayam, "1"))).andExpect(status().isOk());
		mvc.perform(as(post("/api/v1/users"), t,
				"{\"name\":\"Siti\",\"email\":\"cs4@x.id\",\"password\":\"kasir12345\"}")).andExpect(status().isCreated());
		String c = JsonPath.read(body(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"cs4@x.id\",\"password\":\"kasir12345\"}")), "$.data.accessToken");

		mvc.perform(as(get("/api/v1/stock-items"), c, "")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name").value("Ayam"))
				.andExpect(jsonPath("$.data[0].unitCost").doesNotExist());
		mvc.perform(as(get("/api/v1/products"), c, "")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].price").value(13000))
				.andExpect(jsonPath("$.data[0].hpp").doesNotExist())
				.andExpect(jsonPath("$.data[0].margin").doesNotExist())
				.andExpect(jsonPath("$.data[0].suggestedPrice").doesNotExist())
				.andExpect(jsonPath("$.data[0].recipe.length()").value(1));
		mvc.perform(as(post("/api/v1/stock-items"), c, item("X", "PCS", 1))).andExpect(status().isForbidden());
		mvc.perform(as(put("/api/v1/stock-items/" + ayam), c, item("X", "PCS", 1))).andExpect(status().isForbidden());
		mvc.perform(as(delete("/api/v1/stock-items/" + ayam), c, "")).andExpect(status().isForbidden());
		mvc.perform(as(post("/api/v1/products"), c, "{\"name\":\"X\",\"price\":1}")).andExpect(status().isForbidden());
		mvc.perform(as(put("/api/v1/products/" + p + "/recipe"), c, recipe(ayam, "2"))).andExpect(status().isForbidden());
		mvc.perform(as(delete("/api/v1/products/" + p), c, "")).andExpect(status().isForbidden());
	}

	@Test
	void outletsCannotSeeOrTouchEachOthersCatalog() throws Exception {
		String a = ownerToken("c5a@x.id");
		String b = ownerToken("c5b@x.id");
		String ayamA = stockItem(a, item("Ayam", "PCS", 7000));
		String pA = JsonPath.read(body(as(post("/api/v1/products"), a, "{\"name\":\"AG\",\"price\":1}")), "$.data.id");

		mvc.perform(as(get("/api/v1/stock-items"), b, "")).andExpect(jsonPath("$.data.length()").value(0));
		mvc.perform(as(get("/api/v1/products"), b, "")).andExpect(jsonPath("$.data.length()").value(0));
		mvc.perform(as(put("/api/v1/stock-items/" + ayamA), b, item("Hack", "PCS", 1))).andExpect(status().isNotFound());
		mvc.perform(as(delete("/api/v1/stock-items/" + ayamA), b, "")).andExpect(status().isNotFound());
		mvc.perform(as(put("/api/v1/products/" + pA), b, "{\"name\":\"Hack\",\"price\":1}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
		mvc.perform(as(put("/api/v1/products/" + pA + "/recipe"), b, recipe(ayamA, "1")))
				.andExpect(status().isNotFound());
		// nama yang sama boleh dipakai usaha lain
		mvc.perform(as(post("/api/v1/stock-items"), b, item("Ayam", "PCS", 1))).andExpect(status().isCreated());
		// resep usaha B tidak boleh memakai bahan usaha A
		String pB = JsonPath.read(body(as(post("/api/v1/products"), b, "{\"name\":\"BG\",\"price\":1}")), "$.data.id");
		mvc.perform(as(put("/api/v1/products/" + pB + "/recipe"), b, recipe(ayamA, "1")))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"));
	}

}
