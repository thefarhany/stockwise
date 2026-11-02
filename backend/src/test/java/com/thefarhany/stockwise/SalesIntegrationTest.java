package com.thefarhany.stockwise;

import static com.thefarhany.stockwise.ShopFixture.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.thefarhany.stockwise.ShopFixture.Shop;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class SalesIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	TestClock clock;

	private ShopFixture fixture;

	@BeforeEach
	void setUp() {
		fixture = new ShopFixture(mvc);
		clock.set("2026-11-02T04:00:00Z"); // 11:00 WIB
	}

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	private String json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b) throws Exception {
		return fixture.json(b);
	}

	private static String line(String item, String qty, String cost) {
		return "{\"stockItemId\":\"" + item + "\",\"quantity\":" + qty + "," + cost + "}";
	}

	/** Buka sesi: ayam @7.000/pcs, tepung 2 KG (2.000 g), sambal 5 tanpa biaya. */
	private String openSession(Shop s, String ayamQty) throws Exception {
		long ayamCost = Long.parseLong(ayamQty) * 7000;
		return JsonPath.read(json(as(post("/api/v1/sessions/open"), s.owner(),
				"{\"stock\":[" + line(s.ayam(), ayamQty, "\"totalCost\":" + ayamCost) + ","
						+ line(s.tepung(), "2", "\"unit\":\"KG\",\"totalCost\":16000") + ","
						+ line(s.sambal(), "5", "\"noCost\":true") + "]}")), "$.data.id");
	}

	private static String items(String... productQty) {
		List<String> parts = new ArrayList<>();
		for (int i = 0; i < productQty.length; i += 2) {
			parts.add("{\"productId\":\"" + productQty[i] + "\",\"quantity\":" + productQty[i + 1] + "}");
		}
		return "[" + String.join(",", parts) + "]";
	}

	private static String sale(String session, String method, String cash, String items) {
		return "{\"sessionId\":\"" + session + "\",\"paymentMethod\":\"" + method + "\""
				+ (cash == null ? "" : ",\"cashReceived\":" + cash) + ",\"items\":" + items + "}";
	}

	private String sell(Shop s, String token, String sessionId, String method, String cash, String items)
			throws Exception {
		return json(as(post("/api/v1/sales"), token, sale(sessionId, method, cash, items)));
	}

	private static Object stockAfter(String saleJson, String name, String field) {
		List<Object> found = JsonPath.read(saleJson, "$.data.stockAfter[?(@.name=='" + name + "')]." + field);
		return found.get(0);
	}

	private double sold(String sessionId, String itemId) {
		return jdbc.queryForObject("SELECT sold_qty FROM session_stock WHERE session_id = UUID_TO_BIN(?) "
				+ "AND stock_item_id = UUID_TO_BIN(?)", Double.class, sessionId, itemId);
	}

	private int saleCount(String sessionId) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM sale WHERE session_id = UUID_TO_BIN(?)", Integer.class,
				sessionId);
	}

	private String closeAll(String sessionId, Shop s, String ayamQty, String tepungQty, String sambalQty,
			String token) throws Exception {
		return json(as(post("/api/v1/sessions/" + sessionId + "/close"), token, "{\"items\":["
				+ "{\"stockItemId\":\"" + s.ayam() + "\",\"closingQty\":" + ayamQty + ",\"carryOverQty\":" + ayamQty
				+ ",\"discardQty\":0},"
				+ "{\"stockItemId\":\"" + s.tepung() + "\",\"closingQty\":" + tepungQty + ",\"carryOverQty\":"
				+ tepungQty + ",\"discardQty\":0},"
				+ "{\"stockItemId\":\"" + s.sambal() + "\",\"closingQty\":" + sambalQty
				+ ",\"carryOverQty\":0,\"discardQty\":" + sambalQty + "}]}"));
	}

	// --- penjualan ---

	@Test
	void saleReducesStockByRecipeAndKeepsPriceSnapshot() throws Exception {
		Shop s = fixture.create();
		String sid = openSession(s, "10");

		// 2 Ayam Goreng (ayam 2, tepung 200) + 1 Paket (ayam 2, sambal 1): ayam 4 terpakai
		String first = sell(s, s.cashier(), sid, "CASH", "100000", items(s.goreng(), "2", s.paket(), "1"));
		assertThat(JsonPath.<Integer>read(first, "$.data.receiptNo")).isEqualTo(1);
		assertThat(JsonPath.<Integer>read(first, "$.data.totalAmount")).isEqualTo(46000);
		assertThat(JsonPath.<Integer>read(first, "$.data.change")).isEqualTo(54000);
		assertThat(JsonPath.<String>read(first, "$.data.status")).isEqualTo("COMPLETED");
		assertThat(((Number) stockAfter(first, "Ayam", "availableQty")).doubleValue()).isEqualTo(6.0);
		assertThat(((Number) stockAfter(first, "Tepung", "availableQty")).doubleValue()).isEqualTo(1800.0);
		assertThat(((Number) stockAfter(first, "Sambal", "availableQty")).doubleValue()).isEqualTo(4.0);
		assertThat(stockAfter(first, "Ayam", "soldOutAt")).isNull();
		assertThat(JsonPath.<Integer>read(first, "$.data.items[0].unitPrice")).isEqualTo(13000);

		String second = sell(s, s.owner(), sid, "QRIS", null, items(s.goreng(), "1"));
		assertThat(JsonPath.<Integer>read(second, "$.data.receiptNo")).isEqualTo(2);
		assertThat(JsonPath.<Object>read(second, "$.data.change")).isNull();
		assertThat(sold(sid, s.ayam())).isEqualTo(5.0);

		// satu pergerakan SALE per bahan, mengacu ke transaksinya
		String saleId = JsonPath.read(first, "$.data.id");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE type = 'SALE' "
				+ "AND reference_id = UUID_TO_BIN(?)", Integer.class, saleId)).isEqualTo(3);

		// harga naik: riwayat tetap memakai harga saat transaksi
		mvc.perform(as(put("/api/v1/products/" + s.goreng()), s.owner(), "{\"name\":\"Ayam Goreng\",\"price\":15000}"))
				.andExpect(status().isOk());
		mvc.perform(as(get("/api/v1/sales/" + saleId), s.cashier(), ""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.receipt.items[0].unitPrice").value(13000))
				.andExpect(jsonPath("$.data.receipt.businessName").value("Geprek Pak Budi"))
				.andExpect(jsonPath("$.data.receipt.cashierName").value("Siti"))
				.andExpect(jsonPath("$.data.receipt.change").value(54000));
		String third = sell(s, s.owner(), sid, "CASH", null, items(s.goreng(), "1"));
		assertThat(JsonPath.<Integer>read(third, "$.data.totalAmount")).isEqualTo(15000);
		assertThat(JsonPath.<Object>read(third, "$.data.change")).isNull(); // tunai tanpa uang diterima dicatat
	}

	@Test
	void saleValidationAndRejectionsLeaveNothingBehind() throws Exception {
		Shop s = fixture.create();
		String sid = openSession(s, "10");
		String url = "/api/v1/sales";

		mvc.perform(as(post(url), s.cashier(), sale(sid, "CASH", "1000", items(s.goreng(), "1"))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.errorCode").value("CASH_INSUFFICIENT"));
		mvc.perform(as(post(url), s.cashier(), sale(sid, "QRIS", "50000", items(s.goreng(), "1"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.cashReceived").exists());
		mvc.perform(as(post(url), s.cashier(), sale(sid, "CASH", null, items(s.goreng(), "1", s.goreng(), "1"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.items").exists());
		mvc.perform(as(post(url), s.cashier(), sale(sid, "CASH", null, items(s.goreng(), "0"))))
				.andExpect(status().isBadRequest());
		mvc.perform(as(post(url), s.cashier(), sale(sid, "CASH", null, "[]"))).andExpect(status().isBadRequest());
		mvc.perform(as(post(url), s.cashier(),
				sale(sid, "CASH", null, items(UUID.randomUUID().toString(), "1"))))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
		mvc.perform(as(post(url), s.cashier(), sale(sid, "CASH", null, items(s.polos(), "1"))))
				.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errorCode").value("RECIPE_REQUIRED"));
		mvc.perform(as(delete("/api/v1/products/" + s.paket()), s.owner(), "")).andExpect(status().isOk());
		mvc.perform(as(post(url), s.cashier(), sale(sid, "CASH", null, items(s.paket(), "1"))))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
		mvc.perform(as(post(url), s.cashier(), sale(UUID.randomUUID().toString(), "CASH", null, items(s.goreng(), "1"))))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));

		assertThat(saleCount(sid)).isZero();
		assertThat(sold(sid, s.ayam())).isZero();

		// sesi tertutup final
		closeAll(sid, s, "10", "2000", "5", s.owner());
		mvc.perform(as(post(url), s.cashier(), sale(sid, "CASH", null, items(s.goreng(), "1"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_OPEN"));
		mvc.perform(as(post("/api/v1/sales/recap"), s.cashier(), "{\"sessionId\":\"" + sid
				+ "\",\"paymentMethod\":\"CASH\",\"items\":" + items(s.goreng(), "1") + "}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_OPEN"));
	}

	@Test
	void insufficientStockRejectsEverythingThenSoldOutAndVoidRestores() throws Exception {
		Shop s = fixture.create();
		String sid = openSession(s, "3");

		mvc.perform(as(post("/api/v1/sales"), s.cashier(), sale(sid, "CASH", null, items(s.goreng(), "5"))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
				.andExpect(jsonPath("$.details.shortages.length()").value(1))
				.andExpect(jsonPath("$.details.shortages[0].name").value("Ayam"))
				.andExpect(jsonPath("$.details.shortages[0].needed").value(5))
				.andExpect(jsonPath("$.details.shortages[0].available").value(3));
		assertThat(saleCount(sid)).isZero();
		assertThat(sold(sid, s.tepung())).isZero(); // bahan yang cukup pun tidak berubah

		String sellAll = sell(s, s.cashier(), sid, "CASH", null, items(s.goreng(), "3"));
		String saleId = JsonPath.read(sellAll, "$.data.id");
		assertThat(stockAfter(sellAll, "Ayam", "soldOutAt")).isNotNull(); // item utama habis
		mvc.perform(as(post("/api/v1/sales"), s.cashier(), sale(sid, "CASH", null, items(s.goreng(), "1"))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.details.shortages[0].available").value(0));

		mvc.perform(as(post("/api/v1/sales/" + saleId + "/void"), s.cashier(), "{\"reason\":\"Salah tekan\"}"))
				.andExpect(status().isOk());
		String current = json(as(get("/api/v1/sessions/current"), s.owner(), ""));
		List<Object> soldOut = JsonPath.read(current, "$.data.stock[?(@.name=='Ayam')].soldOutAt");
		List<Object> available = JsonPath.read(current, "$.data.stock[?(@.name=='Ayam')].availableQty");
		assertThat(soldOut).hasSize(1);
		assertThat(soldOut.get(0)).as("tanda habis dihapus setelah void").isNull();
		assertThat(((Number) available.get(0)).doubleValue()).isEqualTo(3.0);
	}

	@Test
	void voidRestoresExactlyWhatWasSoldEvenIfRecipeChangedAndCannotRepeat() throws Exception {
		Shop s = fixture.create();
		String sid = openSession(s, "10");
		String made = sell(s, s.owner(), sid, "CASH", null, items(s.goreng(), "2"));
		String saleId = JsonPath.read(made, "$.data.id");
		assertThat(sold(sid, s.ayam())).isEqualTo(2.0);

		// resep berubah (ayam 3 per porsi) sebelum void: pengembalian harus tetap sebesar yang dulu dikurangi
		fixture.recipe(s.owner(), s.goreng(), "{\"stockItemId\":\"" + s.ayam() + "\",\"quantity\":3}");

		String url = "/api/v1/sales/" + saleId + "/void";
		mvc.perform(as(post(url), s.cashier(), "{\"reason\":\"  \"}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(as(post(url), s.cashier(), "{\"reason\":\"Pelanggan batal\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("VOID"))
				.andExpect(jsonPath("$.data.voidReason").value("Pelanggan batal"));
		assertThat(sold(sid, s.ayam())).isZero();
		assertThat(sold(sid, s.tepung())).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE type = 'VOID' "
				+ "AND reference_id = UUID_TO_BIN(?)", Integer.class, saleId)).isEqualTo(2);
		assertThat(jdbc.queryForObject("SELECT voided_by IS NOT NULL AND voided_at IS NOT NULL FROM sale "
				+ "WHERE id = UUID_TO_BIN(?)", Boolean.class, saleId)).isTrue();

		mvc.perform(as(post(url), s.owner(), "{\"reason\":\"lagi\"}")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("SALE_ALREADY_VOID"));
		mvc.perform(as(post("/api/v1/sales/" + UUID.randomUUID() + "/void"), s.owner(), "{\"reason\":\"x\"}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("SALE_NOT_FOUND"));

		// sesi tertutup final: void ditolak; Cashier juga tak bisa lagi melihat detailnya, Owner masih bisa
		String other = JsonPath.read(sell(s, s.owner(), sid, "CASH", null, items(s.goreng(), "1")), "$.data.id");
		closeAll(sid, s, "7", "2000", "5", s.owner());
		mvc.perform(as(post("/api/v1/sales/" + other + "/void"), s.owner(), "{\"reason\":\"terlambat\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_OPEN"));
		mvc.perform(as(get("/api/v1/sales/" + other), s.cashier(), "")).andExpect(status().isForbidden());
		mvc.perform(as(get("/api/v1/sales/" + other), s.owner(), "")).andExpect(status().isOk());
	}

	@Test
	void recapRecordsOneRecapSaleAndFollowsStockRules() throws Exception {
		Shop s = fixture.create();
		String sid = openSession(s, "10");
		String body = "{\"sessionId\":\"" + sid + "\",\"paymentMethod\":\"QRIS\",\"items\":"
				+ items(s.goreng(), "4", s.paket(), "1") + "}";
		String recap = json(as(post("/api/v1/sales/recap"), s.cashier(), body));
		assertThat(JsonPath.<Integer>read(recap, "$.data.totalAmount")).isEqualTo(4 * 13000 + 20000);
		assertThat(jdbc.queryForObject("SELECT input_method FROM sale WHERE id = UUID_TO_BIN(?)", String.class,
				(String) JsonPath.read(recap, "$.data.id"))).isEqualTo("RECAP");
		assertThat(sold(sid, s.ayam())).isEqualTo(6.0);

		mvc.perform(as(post("/api/v1/sales/recap"), s.cashier(), "{\"sessionId\":\"" + sid
				+ "\",\"paymentMethod\":\"CASH\",\"items\":" + items(s.goreng(), "50") + "}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
		assertThat(saleCount(sid)).isEqualTo(1);
	}

	// --- riwayat dan peran ---

	@Test
	void historyOwnerSeesAnyDayCashierOnlyRunningSession() throws Exception {
		Shop s = fixture.create();
		String sid = openSession(s, "20");
		for (int i = 0; i < 3; i++) {
			sell(s, i == 0 ? s.owner() : s.cashier(), sid, "CASH", null, items(s.goreng(), "1", s.paket(), "1"));
		}
		clock.set("2026-11-02T05:00:00Z");

		mvc.perform(as(get("/api/v1/sales?size=2"), s.owner(), ""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].receiptNo").value(3)) // terbaru dulu
				.andExpect(jsonPath("$.data[0].itemCount").value(2))
				.andExpect(jsonPath("$.data[0].cashierName").value("Siti"))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));
		mvc.perform(as(get("/api/v1/sales?size=2&page=1"), s.owner(), ""))
				.andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].receiptNo").value(1))
				.andExpect(jsonPath("$.data[0].cashierName").value("Budi"));
		mvc.perform(as(get("/api/v1/sales?sessionId=" + sid), s.owner(), ""))
				.andExpect(jsonPath("$.totalElements").value(3));
		mvc.perform(as(get("/api/v1/sales?date=2026-11-02"), s.owner(), ""))
				.andExpect(jsonPath("$.totalElements").value(3));
		mvc.perform(as(get("/api/v1/sales?date=2026-10-01"), s.owner(), ""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));

		// Cashier: sesi berjalan saja
		mvc.perform(as(get("/api/v1/sales"), s.cashier(), "")).andExpect(jsonPath("$.totalElements").value(3));
		mvc.perform(as(get("/api/v1/sales?sessionId=" + sid + "&date=2026-11-02"), s.cashier(), ""))
				.andExpect(status().isOk());
		mvc.perform(as(get("/api/v1/sales?date=2026-10-01"), s.cashier(), "")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		mvc.perform(as(get("/api/v1/sales?sessionId=" + UUID.randomUUID()), s.cashier(), ""))
				.andExpect(status().isForbidden());

		// setelah sesi ditutup tidak ada sesi berjalan: Cashier melihat daftar kosong dan tak bisa meminta sesi itu
		closeAll(sid, s, "14", "1700", "2", s.owner());
		mvc.perform(as(get("/api/v1/sales"), s.cashier(), ""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
		mvc.perform(as(get("/api/v1/sales?sessionId=" + sid), s.cashier(), "")).andExpect(status().isForbidden());
		mvc.perform(as(get("/api/v1/sales?sessionId=" + sid), s.owner(), ""))
				.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	void salesAreIsolatedBetweenOutlets() throws Exception {
		Shop a = fixture.create();
		Shop b = fixture.create();
		String sidA = openSession(a, "10");
		String saleA = JsonPath.read(sell(a, a.owner(), sidA, "CASH", null, items(a.goreng(), "1")), "$.data.id");

		mvc.perform(as(get("/api/v1/sales/" + saleA), b.owner(), "")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("SALE_NOT_FOUND"));
		mvc.perform(as(post("/api/v1/sales/" + saleA + "/void"), b.owner(), "{\"reason\":\"x\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(as(post("/api/v1/sales"), b.owner(), sale(sidA, "CASH", null, items(b.goreng(), "1"))))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));
		mvc.perform(as(get("/api/v1/sales?sessionId=" + sidA), b.owner(), "")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));
		// produk usaha lain tidak bisa dijual di sesi sendiri
		String sidB = openSession(b, "10");
		mvc.perform(as(post("/api/v1/sales"), b.owner(), sale(sidB, "CASH", null, items(a.goreng(), "1"))))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
		assertThat(saleCount(sidA)).isEqualTo(1);
	}

	// --- bersamaan ---

	@Test
	void concurrentSalesNeverOversellAndReceiptNumbersAreUnique() throws Exception {
		Shop s = fixture.create();
		String sid = openSession(s, "3");

		ExecutorService pool = Executors.newFixedThreadPool(5);
		CountDownLatch go = new CountDownLatch(1);
		List<Future<Integer>> results = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			results.add(pool.submit(() -> {
				go.await();
				return mvc.perform(as(post("/api/v1/sales"), s.cashier(),
						sale(sid, "CASH", null, items(s.goreng(), "1")))).andReturn().getResponse().getStatus();
			}));
		}
		go.countDown();
		List<Integer> codes = new ArrayList<>();
		for (Future<Integer> f : results) {
			codes.add(f.get());
		}
		pool.shutdown();

		assertThat(codes.stream().filter(c -> c == 201).count()).isEqualTo(3);
		assertThat(codes.stream().filter(c -> c == 422).count()).isEqualTo(2);
		assertThat(sold(sid, s.ayam())).isEqualTo(3.0);
		assertThat(jdbc.queryForList("SELECT receipt_no FROM sale WHERE session_id = UUID_TO_BIN(?) "
				+ "ORDER BY receipt_no", Integer.class, sid)).containsExactly(1, 2, 3);
	}

}
