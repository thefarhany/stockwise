package com.thefarhany.stockwise;

import static com.thefarhany.stockwise.ApiCases.delete;
import static com.thefarhany.stockwise.ApiCases.get;
import static com.thefarhany.stockwise.ApiCases.post;
import static com.thefarhany.stockwise.ApiCases.put;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.thefarhany.stockwise.ShopFixture.Shop;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Katalog bahan dan produk: input salah, batas nilai, nama aneh, hak akses, isolasi antar usaha. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CatalogEdgeCasesIntegrationTest {

	@Autowired
	MockMvc mvc;

	private ShopFixture fixture;
	private Shop s;

	@BeforeEach
	void setUp() throws Exception {
		fixture = new ShopFixture(mvc);
		s = fixture.create();
	}

	private static String item(String name, String unit, String extra) {
		return "{\"name\":\"" + name + "\",\"kind\":\"RAW\",\"unit\":\"" + unit + "\"" + extra + "}";
	}

	private static String product(String name, String extra) {
		return "{\"name\":\"" + name + "\"" + extra + "}";
	}

	private static String recipe(String stockItemId, String quantity) {
		return "{\"items\":[{\"stockItemId\":\"" + stockItemId + "\",\"quantity\":" + quantity + "}]}";
	}

	@Test
	void stockItemInputValidation() throws Exception {
		new ApiCases(mvc)
				.ok("bahan minimal", 201, post("/api/v1/stock-items", s.owner(), item("Garam", "GRAM", "")))
				.fail("nama kosong", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("", "GRAM", "")))
				.fail("nama hanya spasi", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("   ", "GRAM", "")))
				.fail("nama 101 karakter", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("a".repeat(101), "GRAM", "")))
				.ok("nama 100 karakter", 201, post("/api/v1/stock-items", s.owner(), item("b".repeat(100), "GRAM", "")))
				.fail("satuan tidak dikenal", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "LUSIN", "")))
				.fail("jenis tidak dikenal", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), "{\"name\":\"X\",\"kind\":\"BASI\",\"unit\":\"GRAM\"}"))
				.fail("tanpa satuan", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), "{\"name\":\"X\",\"kind\":\"RAW\"}"))
				.fail("harga satuan negatif", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "GRAM", ",\"unitCost\":-1")))
				.fail("harga satuan 1 miliar + 1", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "GRAM", ",\"unitCost\":1000000001")))
				.fail("harga satuan 9e18", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "GRAM", ",\"unitCost\":9000000000000000000")))
				.fail("harga satuan lewat batas Long", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "GRAM", ",\"unitCost\":99999999999999999999")))
				.fail("harga satuan pecahan", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "GRAM", ",\"unitCost\":10.5")))
				.fail("harga satuan berupa teks", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "GRAM", ",\"unitCost\":\"mahal\"")))
				.ok("harga satuan tepat 1 miliar", 201, post("/api/v1/stock-items", s.owner(), item("Mahal", "GRAM", ",\"unitCost\":1000000000")))
				.fail("umur simpan 0", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "GRAM", ",\"shelfLifeDays\":0")))
				.fail("umur simpan 366", 400, "VALIDATION_ERROR", post("/api/v1/stock-items", s.owner(), item("X", "GRAM", ",\"shelfLifeDays\":366")))
				.ok("umur simpan 365", 201, post("/api/v1/stock-items", s.owner(), item("Beras", "KG", ",\"shelfLifeDays\":365")))
				.fail("id tidak ada", 404, "STOCK_ITEM_NOT_FOUND", put("/api/v1/stock-items/" + UUID.randomUUID(), s.owner(), item("X", "GRAM", "")))
				.fail("id bukan UUID", 400, "VALIDATION_ERROR", put("/api/v1/stock-items/abc", s.owner(), item("X", "GRAM", "")))
				.fail("hapus id tidak ada", 404, "STOCK_ITEM_NOT_FOUND", delete("/api/v1/stock-items/" + UUID.randomUUID(), s.owner(), null))
				.runAll();
	}

	@Test
	void unusualNamesAreStoredLiterally() throws Exception {
		List<String> names = List.of("Ayam 🍗 Geprek", "O'Brien \\\"Special\\\"", "x'; DROP TABLE product; --", "<script>alert(1)</script>",
				"Sambal Pedas (level 5)");
		for (String name : names) {
			String body = fixture.json(post("/api/v1/products", s.owner(), product(name, ",\"price\":1000")));
			assertThat(JsonPath.<String>read(body, "$.data.name")).isEqualTo(name.replace("\\\"", "\""));
		}
		String list = fixture.json(get("/api/v1/products", s.owner()));
		assertThat(JsonPath.<List<String>>read(list, "$.data[*].name")).contains("x'; DROP TABLE product; --", "Ayam 🍗 Geprek");
		new ApiCases(mvc).ok("tabel produk masih utuh", 200, get("/api/v1/products", s.owner())).runAll();
	}

	@Test
	void productInputValidationAndMoneyBounds() throws Exception {
		new ApiCases(mvc)
				.ok("harga 0 (gratis) sah", 201, post("/api/v1/products", s.owner(), product("Gratis", ",\"price\":0")))
				.ok("harga tepat 1 miliar", 201, post("/api/v1/products", s.owner(), product("Termahal", ",\"price\":1000000000")))
				.fail("harga 1 miliar + 1", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":1000000001")))
				.fail("harga 9e18 (dulu bikin total penjualan minus)", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":9000000000000000000")))
				.fail("harga negatif", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":-1")))
				.fail("tanpa harga", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", "")))
				.fail("harga null", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":null")))
				.fail("harga pecahan", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":100.5")))
				.fail("nama kosong", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("", ",\"price\":1000")))
				.fail("kategori 51 karakter", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":1000,\"category\":\"" + "k".repeat(51) + "\"")))
				.fail("urutan -1", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":1000,\"sortOrder\":-1")))
				.fail("urutan 32001", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":1000,\"sortOrder\":32001")))
				.fail("mode stok tidak dikenal", 400, "VALIDATION_ERROR", post("/api/v1/products", s.owner(), product("X", ",\"price\":1000,\"stockMode\":\"MAGIC\"")))
				.fail("id tidak ada", 404, "PRODUCT_NOT_FOUND", put("/api/v1/products/" + UUID.randomUUID(), s.owner(), product("X", ",\"price\":1000")))
				.fail("hapus id tidak ada", 404, "PRODUCT_NOT_FOUND", delete("/api/v1/products/" + UUID.randomUUID(), s.owner(), null))
				.runAll();
	}

	@Test
	void recipeLimitsAndReferences() throws Exception {
		String url = "/api/v1/products/" + s.polos() + "/recipe";
		String big = "{\"items\":[" + ("{\"stockItemId\":\"" + s.ayam() + "\",\"quantity\":1},").repeat(50)
				+ "{\"stockItemId\":\"" + s.ayam() + "\",\"quantity\":1}]}";
		new ApiCases(mvc)
				.ok("resep valid", 200, put(url, s.owner(), recipe(s.ayam(), "2.5")))
				.ok("resep dikosongkan", 200, put(url, s.owner(), "{\"items\":[]}"))
				.fail("jumlah 0", 400, "VALIDATION_ERROR", put(url, s.owner(), recipe(s.ayam(), "0")))
				.fail("jumlah negatif", 400, "VALIDATION_ERROR", put(url, s.owner(), recipe(s.ayam(), "-1")))
				.fail("jumlah > 1 juta", 400, "VALIDATION_ERROR", put(url, s.owner(), recipe(s.ayam(), "1000001")))
				.ok("jumlah tepat 1 juta", 200, put(url, s.owner(), recipe(s.ayam(), "1000000")))
				.fail("presisi > 3 desimal", 400, "VALIDATION_ERROR", put(url, s.owner(), recipe(s.ayam(), "0.0001")))
				.ok("presisi 3 desimal", 200, put(url, s.owner(), recipe(s.ayam(), "0.001")))
				.fail("bahan tidak ada", 404, "STOCK_ITEM_NOT_FOUND", put(url, s.owner(), recipe(UUID.randomUUID().toString(), "1")))
				.fail("tanpa daftar bahan", 400, "VALIDATION_ERROR", put(url, s.owner(), "{}"))
				.fail("bahan null", 400, "VALIDATION_ERROR", put(url, s.owner(), "{\"items\":[null]}"))
				.fail("lebih dari 50 baris", 400, "VALIDATION_ERROR", put(url, s.owner(), big))
				.fail("produk tidak ada", 404, "PRODUCT_NOT_FOUND", put("/api/v1/products/" + UUID.randomUUID() + "/recipe", s.owner(), recipe(s.ayam(), "1")))
				.runAll();
	}

	@Test
	void largestAllowedPriceStillComputesCorrectly() throws Exception {
		fixture.json(put("/api/v1/products/" + s.polos(), s.owner(), product("Polos", ",\"price\":999999999")));
		String list = fixture.json(get("/api/v1/products", s.owner()));
		assertThat(JsonPath.<List<Number>>read(list, "$.data[?(@.id=='" + s.polos() + "')].price").get(0).longValue())
				.isEqualTo(999_999_999L);
	}

	@Test
	void cashierCanReadButNotWriteCatalog() throws Exception {
		new ApiCases(mvc)
				.ok("kasir lihat produk", 200, get("/api/v1/products", s.cashier()))
				.ok("kasir lihat bahan", 200, get("/api/v1/stock-items", s.cashier()))
				.fail("kasir buat produk", 403, "FORBIDDEN", post("/api/v1/products", s.cashier(), product("X", ",\"price\":1000")))
				.fail("kasir ubah produk", 403, "FORBIDDEN", put("/api/v1/products/" + s.polos(), s.cashier(), product("X", ",\"price\":1000")))
				.fail("kasir hapus produk", 403, "FORBIDDEN", delete("/api/v1/products/" + s.polos(), s.cashier(), null))
				.fail("kasir ubah resep", 403, "FORBIDDEN", put("/api/v1/products/" + s.polos() + "/recipe", s.cashier(), recipe(s.ayam(), "1")))
				.fail("kasir buat bahan", 403, "FORBIDDEN", post("/api/v1/stock-items", s.cashier(), item("X", "GRAM", "")))
				.fail("kasir ubah bahan", 403, "FORBIDDEN", put("/api/v1/stock-items/" + s.ayam(), s.cashier(), item("X", "GRAM", "")))
				.fail("kasir hapus bahan", 403, "FORBIDDEN", delete("/api/v1/stock-items/" + s.ayam(), s.cashier(), null))
				.fail("tanpa token", 401, "UNAUTHORIZED", get("/api/v1/products", null))
				.runAll();
	}

	@Test
	void anotherOutletCannotSeeOrTouchMyCatalog() throws Exception {
		Shop other = fixture.create();
		new ApiCases(mvc)
				.fail("ubah produk usaha lain", 404, "PRODUCT_NOT_FOUND", put("/api/v1/products/" + s.polos(), other.owner(), product("Hack", ",\"price\":1")))
				.fail("hapus produk usaha lain", 404, "PRODUCT_NOT_FOUND", delete("/api/v1/products/" + s.polos(), other.owner(), null))
				.fail("ubah bahan usaha lain", 404, "STOCK_ITEM_NOT_FOUND", put("/api/v1/stock-items/" + s.ayam(), other.owner(), item("Hack", "GRAM", "")))
				.fail("hapus bahan usaha lain", 404, "STOCK_ITEM_NOT_FOUND", delete("/api/v1/stock-items/" + s.ayam(), other.owner(), null))
				.fail("resep produk usaha lain", 404, "PRODUCT_NOT_FOUND", put("/api/v1/products/" + s.polos() + "/recipe", other.owner(), recipe(other.ayam(), "1")))
				.fail("resep pakai bahan usaha lain", 404, "STOCK_ITEM_NOT_FOUND", put("/api/v1/products/" + other.polos() + "/recipe", other.owner(), recipe(s.ayam(), "1")))
				.runAll();
		assertThat(JsonPath.<List<String>>read(fixture.json(get("/api/v1/products", s.owner())), "$.data[*].name")).doesNotContain("Hack");
	}

}
