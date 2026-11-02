package com.thefarhany.stockwise;

import static com.thefarhany.stockwise.ApiCases.get;
import static com.thefarhany.stockwise.ApiCases.post;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.thefarhany.stockwise.ShopFixture.Shop;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Sesi harian dan penjualan: input salah, batas nilai, urutan status, hak akses, isolasi usaha. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SessionSalesEdgeCasesIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestClock clock;

	private ShopFixture fixture;
	private Shop s;

	@BeforeEach
	void setUp() throws Exception {
		fixture = new ShopFixture(mvc);
		clock.set("2026-11-02T04:00:00Z"); // 11:00 WIB
		s = fixture.create();
	}

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	private static String line(String stockItemId, String qty, String extra) {
		return "{\"stockItemId\":\"" + stockItemId + "\",\"quantity\":" + qty + extra + "}";
	}

	private static String open(String... lines) {
		return "{\"stock\":[" + String.join(",", lines) + "]}";
	}

	private static String sale(String sessionId, String method, String extra, String... items) {
		return "{\"sessionId\":\"" + sessionId + "\",\"paymentMethod\":\"" + method + "\"" + extra + ",\"items\":["
				+ String.join(",", items) + "]}";
	}

	private static String saleItem(String productId, String qty) {
		return "{\"productId\":\"" + productId + "\",\"quantity\":" + qty + "}";
	}

	@Test
	void openSessionRejectsBadStockLines() throws Exception {
		String a = s.ayam();
		new ApiCases(mvc)
				.fail("jumlah 0", 400, "VALIDATION_ERROR", post("/api/v1/sessions/open", s.owner(), open(line(a, "0", ",\"totalCost\":1000"))))
				.fail("jumlah negatif", 400, "VALIDATION_ERROR", post("/api/v1/sessions/open", s.owner(), open(line(a, "-1", ",\"totalCost\":1000"))))
				.fail("jumlah 4 desimal", 400, "VALIDATION_ERROR", post("/api/v1/sessions/open", s.owner(), open(line(a, "1.0001", ",\"totalCost\":1000"))))
				.fail("tanpa biaya dan tanpa noCost", 400, "COST_REQUIRED", post("/api/v1/sessions/open", s.owner(), open(line(a, "1", ""))))
				.fail("totalCost 0", 400, "COST_REQUIRED", post("/api/v1/sessions/open", s.owner(), open(line(a, "1", ",\"totalCost\":0"))))
				.fail("totalCost > 1 triliun", 400, "VALIDATION_ERROR", post("/api/v1/sessions/open", s.owner(), open(line(a, "1", ",\"totalCost\":1000000000001"))))
				.fail("totalCost 9e18 (dulu 409)", 400, "VALIDATION_ERROR", post("/api/v1/sessions/open", s.owner(), open(line(a, "1", ",\"totalCost\":9000000000000000000"))))
				.fail("harga per satuan melewati batas (1 pcs = 1 triliun)", 400, "VALIDATION_ERROR", post("/api/v1/sessions/open", s.owner(), open(line(a, "1", ",\"totalCost\":1000000000000"))))
				.fail("satuan beda keluarga (PCS dibeli KG)", 422, "UNIT_MISMATCH", post("/api/v1/sessions/open", s.owner(), open(line(a, "1", ",\"unit\":\"KG\",\"totalCost\":1000"))))
				.fail("bahan tidak ada", 404, "STOCK_ITEM_NOT_FOUND", post("/api/v1/sessions/open", s.owner(), open(line(UUID.randomUUID().toString(), "1", ",\"noCost\":true"))))
				.fail("stockItemId kosong", 400, "VALIDATION_ERROR", post("/api/v1/sessions/open", s.owner(), "{\"stock\":[{\"quantity\":1,\"noCost\":true}]}"))
				.fail("baris null", 400, "VALIDATION_ERROR", post("/api/v1/sessions/open", s.owner(), "{\"stock\":[null]}"))
				.runAll();
		// semua penolakan di atas tidak boleh meninggalkan sesi setengah jadi
		assertThat(JsonPath.<Object>read(fixture.json(get("/api/v1/sessions/current", s.owner())), "$.data")).isNull();
	}

	@Test
	void sessionLifecycleOrderIsEnforced() throws Exception {
		String sid = fixture.open(s.owner(), s);
		String badClose = "{\"items\":[{\"stockItemId\":\"" + s.ayam() + "\",\"closingQty\":5,\"carryOverQty\":2,\"discardQty\":1}]}";
		new ApiCases(mvc)
				.fail("buka dua kali", 409, "SESSION_ALREADY_OPEN", post("/api/v1/sessions/open", s.owner(), "{}"))
				.fail("tutup: carry + discard != closing", 400, "VALIDATION_ERROR", post("/api/v1/sessions/" + sid + "/close", s.owner(), badClose))
				.fail("tutup: bahan tidak ada", 404, "STOCK_ITEM_NOT_FOUND", post("/api/v1/sessions/" + sid + "/close", s.owner(),
						"{\"items\":[{\"stockItemId\":\"" + UUID.randomUUID() + "\",\"closingQty\":1,\"carryOverQty\":1,\"discardQty\":0}]}"))
				.fail("tutup: qty negatif", 400, "VALIDATION_ERROR", post("/api/v1/sessions/" + sid + "/close", s.owner(),
						"{\"items\":[{\"stockItemId\":\"" + s.ayam() + "\",\"closingQty\":-1,\"carryOverQty\":0,\"discardQty\":-1}]}"))
				.fail("tutup: catatan 1001 karakter", 400, "VALIDATION_ERROR", post("/api/v1/sessions/" + sid + "/close", s.owner(),
						"{\"items\":[],\"notes\":\"" + "n".repeat(1001) + "\"}"))
				.fail("tutup: tanpa daftar", 400, "VALIDATION_ERROR", post("/api/v1/sessions/" + sid + "/close", s.owner(), "{}"))
				.fail("tutup: id tidak ada", 404, "SESSION_NOT_FOUND", post("/api/v1/sessions/" + UUID.randomUUID() + "/close", s.owner(), "{\"items\":[]}"))
				.fail("tutup: id bukan UUID", 400, "VALIDATION_ERROR", post("/api/v1/sessions/xyz/close", s.owner(), "{\"items\":[]}"))
				.fail("tambah stok: daftar kosong", 400, "VALIDATION_ERROR", post("/api/v1/sessions/" + sid + "/stock", s.owner(), "{\"items\":[]}"))
				.ok("tambah stok sah", 200, post("/api/v1/sessions/" + sid + "/stock", s.owner(), "{\"items\":[" + line(s.ayam(), "1", ",\"totalCost\":7000") + "]}"))
				.runAll();
		clock.set("2026-11-02T14:00:00Z");
		fixture.close(s.owner(), sid, s, "10", "2000", "5");
		new ApiCases(mvc)
				.fail("tutup dua kali", 409, "SESSION_NOT_OPEN", post("/api/v1/sessions/" + sid + "/close", s.owner(), "{\"items\":[]}"))
				.fail("tambah stok setelah tutup", 409, "SESSION_NOT_OPEN", post("/api/v1/sessions/" + sid + "/stock", s.owner(), "{\"items\":[" + line(s.ayam(), "1", ",\"noCost\":true") + "]}"))
				.fail("jual setelah tutup", 409, "SESSION_NOT_OPEN", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "", saleItem(s.goreng(), "1"))))
				.runAll();
	}

	@Test
	void salesRejectBadInput() throws Exception {
		String sid = fixture.open(s.owner(), s);
		String polos = saleItem(s.goreng(), "1");
		new ApiCases(mvc)
				.ok("jual tunai", 201, post("/api/v1/sales", s.cashier(), sale(sid, "CASH", ",\"cashReceived\":13000", polos)))
				.ok("metode OTHER", 201, post("/api/v1/sales", s.cashier(), sale(sid, "OTHER", "", polos)))
				.ok("metode QRIS", 201, post("/api/v1/sales", s.cashier(), sale(sid, "QRIS", "", polos)))
				.fail("metode tidak dikenal", 400, "VALIDATION_ERROR", post("/api/v1/sales", s.cashier(), sale(sid, "KREDIT", "", polos)))
				.fail("tanpa item", 400, "VALIDATION_ERROR", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "")))
				.fail("qty 0", 400, "VALIDATION_ERROR", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "", saleItem(s.goreng(), "0"))))
				.fail("qty negatif", 400, "VALIDATION_ERROR", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "", saleItem(s.goreng(), "-3"))))
				.fail("qty 100001", 400, "VALIDATION_ERROR", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "", saleItem(s.goreng(), "100001"))))
				.fail("qty pecahan", 400, "VALIDATION_ERROR", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "", saleItem(s.goreng(), "1.5"))))
				.fail("produk tidak ada", 404, "PRODUCT_NOT_FOUND", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "", saleItem(UUID.randomUUID().toString(), "1"))))
				.fail("sesi tidak ada", 404, "SESSION_NOT_FOUND", post("/api/v1/sales", s.cashier(), sale(UUID.randomUUID().toString(), "CASH", "", polos)))
				.fail("uang diterima kurang dari total", 422, "CASH_INSUFFICIENT", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", ",\"cashReceived\":12999", polos)))
				.fail("uang diterima negatif", 400, "VALIDATION_ERROR", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", ",\"cashReceived\":-1", polos)))
				.fail("uang diterima > 1 triliun", 400, "VALIDATION_ERROR", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", ",\"cashReceived\":1000000000001", polos)))
				.fail("tanpa token", 401, "UNAUTHORIZED", post("/api/v1/sales", null, sale(sid, "CASH", "", polos)))
				.fail("stok tidak cukup (ayam 10 pcs, minta 11 goreng)", 422, "INSUFFICIENT_STOCK", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "", saleItem(s.goreng(), "11"))))
				.runAll();
	}

	@Test
	void totalNeverOverflowsAtTheLargestAllowedPriceAndQuantity() throws Exception {
		String sid = fixture.open(s.owner(), s);
		fixture.json(ApiCases.put("/api/v1/products/" + s.polos(), s.owner(), "{\"name\":\"Polos\",\"price\":1000000000}"));
		fixture.recipe(s.owner(), s.polos(), "{\"stockItemId\":\"" + s.tepung() + "\",\"quantity\":0.001}");
		// 100.000 x 1 miliar = 1e14 masih muat di Long; hasilnya harus positif dan tepat
		String body = fixture.json(post("/api/v1/sales", s.cashier(), sale(sid, "OTHER", "", saleItem(s.polos(), "100000"))));
		assertThat(JsonPath.<Number>read(body, "$.data.totalAmount").longValue()).isEqualTo(100_000_000_000_000L);
	}

	@Test
	void voidRulesAndPermissions() throws Exception {
		String sid = fixture.open(s.owner(), s);
		String tx = fixture.sell(s.cashier(), sid, s.goreng(), 1);
		String tx2 = fixture.sell(s.cashier(), sid, s.goreng(), 1);
		new ApiCases(mvc)
				.fail("alasan kosong", 400, "VALIDATION_ERROR", post("/api/v1/sales/" + tx + "/void", s.cashier(), "{\"reason\":\"\"}"))
				.fail("alasan spasi saja", 400, "VALIDATION_ERROR", post("/api/v1/sales/" + tx + "/void", s.cashier(), "{\"reason\":\"   \"}"))
				.fail("alasan 201 karakter", 400, "VALIDATION_ERROR", post("/api/v1/sales/" + tx + "/void", s.cashier(), "{\"reason\":\"" + "r".repeat(201) + "\"}"))
				.fail("tanpa body", 400, "VALIDATION_ERROR", post("/api/v1/sales/" + tx + "/void", s.cashier(), null))
				.fail("transaksi tidak ada", 404, "SALE_NOT_FOUND", post("/api/v1/sales/" + UUID.randomUUID() + "/void", s.cashier(), "{\"reason\":\"salah\"}"))
				.ok("batalkan oleh kasir", 200, post("/api/v1/sales/" + tx + "/void", s.cashier(), "{\"reason\":\"salah input\"}"))
				.fail("batalkan dua kali", 409, "SALE_ALREADY_VOID", post("/api/v1/sales/" + tx + "/void", s.owner(), "{\"reason\":\"lagi\"}"))
				.ok("batalkan oleh Owner", 200, post("/api/v1/sales/" + tx2 + "/void", s.owner(), "{\"reason\":\"pelanggan batal\"}"))
				.runAll();
	}

	@Test
	void historyFiltersAndPagingAreSafe() throws Exception {
		String sid = fixture.open(s.owner(), s);
		fixture.sell(s.cashier(), sid, s.goreng(), 1);
		new ApiCases(mvc)
				.ok("riwayat per sesi", 200, get("/api/v1/sales?sessionId=" + sid, s.cashier()))
				.fail("kasir minta tanggal lain", 403, "FORBIDDEN", get("/api/v1/sales?date=2026-10-01", s.cashier()))
				.ok("riwayat per tanggal", 200, get("/api/v1/sales?date=2026-11-02", s.owner()))
				.fail("tanggal salah format", 400, "VALIDATION_ERROR", get("/api/v1/sales?date=02-11-2026", s.owner()))
				.fail("tanggal 30 Februari", 400, "VALIDATION_ERROR", get("/api/v1/sales?date=2026-02-30", s.owner()))
				.fail("sessionId bukan UUID", 400, "VALIDATION_ERROR", get("/api/v1/sales?sessionId=abc", s.owner()))
				.fail("page bukan angka", 400, "VALIDATION_ERROR", get("/api/v1/sales?page=satu", s.owner()))
				.ok("page negatif tidak meledak", 200, get("/api/v1/sales?page=-1", s.owner()))
				.ok("size 0 tidak meledak", 200, get("/api/v1/sales?size=0", s.owner()))
				.ok("size 1 juta tidak meledak", 200, get("/api/v1/sales?size=1000000", s.owner()))
				.ok("halaman jauh di luar data", 200, get("/api/v1/sales?page=99999", s.owner()))
				.fail("transaksi tidak ada", 404, "SALE_NOT_FOUND", get("/api/v1/sales/" + UUID.randomUUID(), s.owner()))
				.runAll();
	}

	@Test
	void anotherOutletCannotReachMySessionOrSales() throws Exception {
		String sid = fixture.open(s.owner(), s);
		String tx = fixture.sell(s.cashier(), sid, s.goreng(), 1);
		Shop other = fixture.create();
		new ApiCases(mvc)
				.fail("lihat sesi usaha lain", 404, "SESSION_NOT_FOUND", get("/api/v1/sessions/" + sid, other.owner()))
				.fail("tutup sesi usaha lain", 404, "SESSION_NOT_FOUND", post("/api/v1/sessions/" + sid + "/close", other.owner(), "{\"items\":[]}"))
				.fail("tambah stok ke sesi usaha lain", 404, "SESSION_NOT_FOUND", post("/api/v1/sessions/" + sid + "/stock", other.owner(), "{\"items\":[" + line(other.ayam(), "1", ",\"noCost\":true") + "]}"))
				.fail("jual ke sesi usaha lain", 404, "SESSION_NOT_FOUND", post("/api/v1/sales", other.cashier(), sale(sid, "CASH", "", saleItem(other.polos(), "1"))))
				.fail("lihat transaksi usaha lain", 404, "SALE_NOT_FOUND", get("/api/v1/sales/" + tx, other.owner()))
				.fail("batalkan transaksi usaha lain", 404, "SALE_NOT_FOUND", post("/api/v1/sales/" + tx + "/void", other.owner(), "{\"reason\":\"iseng\"}"))
				.fail("stok usaha lain dipakai di sesiku", 404, "STOCK_ITEM_NOT_FOUND", post("/api/v1/sessions/" + sid + "/stock", s.owner(), "{\"items\":[" + line(other.ayam(), "1", ",\"noCost\":true") + "]}"))
				.fail("produk usaha lain dijual di sesiku", 404, "PRODUCT_NOT_FOUND", post("/api/v1/sales", s.cashier(), sale(sid, "CASH", "", saleItem(other.polos(), "1"))))
				.runAll();
		assertThat(JsonPath.<Number>read(fixture.json(get("/api/v1/sales?sessionId=" + sid, s.owner())), "$.totalElements").intValue()).isEqualTo(1);
	}

}
