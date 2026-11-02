package com.thefarhany.stockwise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Membuat usaha lengkap lewat endpoint (bukan SQL) untuk tes yang butuh katalog:
 * Ayam (PCS, utama), Tepung (GRAM), Sambal (PCS, tidak boleh dibawa);
 * produk Ayam Goreng (13.000: ayam 1 + tepung 100), Paket (20.000: ayam 2 + sambal 1), Polos (5.000, tanpa resep).
 */
class ShopFixture {

	record Shop(String owner, String cashier, String ayam, String tepung, String sambal, String goreng,
			String paket, String polos) {
	}

	private final MockMvc mvc;

	ShopFixture(MockMvc mvc) {
		this.mvc = mvc;
	}

	static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, String token, String body) {
		return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	String json(MockHttpServletRequestBuilder b) throws Exception {
		return mvc.perform(b).andReturn().getResponse().getContentAsString();
	}

	private String id(String token, String path, String body) throws Exception {
		return JsonPath.read(json(as(post(path), token, body)), "$.data.id");
	}

	Shop create() throws Exception {
		String email = "o-" + UUID.randomUUID() + "@x.id";
		String owner = JsonPath.read(json(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Budi\",\"email\":\"" + email + "\",\"password\":\"rahasia123\","
						+ "\"outletName\":\"Geprek Pak Budi\"}")), "$.data.accessToken");
		mvc.perform(as(put("/api/v1/outlet"), owner,
				"{\"name\":\"Geprek Pak Budi\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\","
						+ "\"paydayDays\":[25,1],\"recommendationBufferPct\":5}")).andExpect(status().isOk());
		String ayam = id(owner, "/api/v1/stock-items",
				"{\"name\":\"Ayam\",\"kind\":\"RAW\",\"unit\":\"PCS\",\"unitCost\":7000,\"isPrimary\":true}");
		String tepung = id(owner, "/api/v1/stock-items", "{\"name\":\"Tepung\",\"kind\":\"RAW\",\"unit\":\"GRAM\"}");
		String sambal = id(owner, "/api/v1/stock-items",
				"{\"name\":\"Sambal\",\"kind\":\"RAW\",\"unit\":\"PCS\",\"carryOverAllowed\":false}");
		String goreng = id(owner, "/api/v1/products", "{\"name\":\"Ayam Goreng\",\"price\":13000}");
		String paket = id(owner, "/api/v1/products", "{\"name\":\"Paket\",\"price\":20000}");
		String polos = id(owner, "/api/v1/products", "{\"name\":\"Polos\",\"price\":5000}");
		recipe(owner, goreng, "{\"stockItemId\":\"" + ayam + "\",\"quantity\":1},{\"stockItemId\":\"" + tepung
				+ "\",\"quantity\":100}");
		recipe(owner, paket, "{\"stockItemId\":\"" + ayam + "\",\"quantity\":2},{\"stockItemId\":\"" + sambal
				+ "\",\"quantity\":1}");
		String cashierEmail = "k-" + UUID.randomUUID() + "@x.id";
		mvc.perform(as(post("/api/v1/users"), owner,
				"{\"name\":\"Siti\",\"email\":\"" + cashierEmail + "\",\"password\":\"kasir12345\"}"))
				.andExpect(status().isCreated());
		String cashier = JsonPath.read(json(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + cashierEmail + "\",\"password\":\"kasir12345\"}")), "$.data.accessToken");
		return new Shop(owner, cashier, ayam, tepung, sambal, goreng, paket, polos);
	}

	/** Tutup sesi: ayam dan tepung dibawa semua, sambal (tidak boleh dibawa) dibuang. */
	String close(String token, String sessionId, Shop s, String ayamQty, String tepungQty, String sambalQty)
			throws Exception {
		return json(as(post("/api/v1/sessions/" + sessionId + "/close"), token, "{\"items\":["
				+ "{\"stockItemId\":\"" + s.ayam() + "\",\"closingQty\":" + ayamQty + ",\"carryOverQty\":" + ayamQty
				+ ",\"discardQty\":0},"
				+ "{\"stockItemId\":\"" + s.tepung() + "\",\"closingQty\":" + tepungQty + ",\"carryOverQty\":"
				+ tepungQty + ",\"discardQty\":0},"
				+ "{\"stockItemId\":\"" + s.sambal() + "\",\"closingQty\":" + sambalQty
				+ ",\"carryOverQty\":0,\"discardQty\":" + sambalQty + "}]}"));
	}

	/** Buka: ayam 10 (Rp7.000/pcs), tepung 2 KG (Rp8/gram), sambal 5 tanpa biaya. Mengembalikan id sesi. */
	String open(String token, Shop s) throws Exception {
		return JsonPath.read(json(as(post("/api/v1/sessions/open"), token, "{\"stock\":["
				+ "{\"stockItemId\":\"" + s.ayam() + "\",\"quantity\":10,\"totalCost\":70000},"
				+ "{\"stockItemId\":\"" + s.tepung() + "\",\"quantity\":2,\"unit\":\"KG\",\"totalCost\":16000},"
				+ "{\"stockItemId\":\"" + s.sambal() + "\",\"quantity\":5,\"noCost\":true}]}")), "$.data.id");
	}

	/** Penjualan tunai tanpa uang diterima. Mengembalikan id transaksi. */
	String sell(String token, String sessionId, String productId, int qty) throws Exception {
		return JsonPath.read(json(as(post("/api/v1/sales"), token, "{\"sessionId\":\"" + sessionId
				+ "\",\"paymentMethod\":\"CASH\",\"items\":[{\"productId\":\"" + productId + "\",\"quantity\":"
				+ qty + "}]}")), "$.data.id");
	}

	/** Satu hari lengkap pada 2 Nov 2026: buka 11:00 WIB, jual `goreng` Ayam Goreng, tutup manual 21:00 WIB. */
	String closedDay(Shop s, TestClock clock, int goreng) throws Exception {
		clock.set("2026-11-02T04:00:00Z");
		String sid = open(s.owner(), s);
		clock.set("2026-11-02T05:00:00Z");
		if (goreng > 0) {
			sell(s.cashier(), sid, s.goreng(), goreng);
		}
		clock.set("2026-11-02T14:00:00Z");
		close(s.owner(), sid, s, String.valueOf(10 - goreng), String.valueOf(2000 - 100 * goreng), "5");
		return sid;
	}

	void recipe(String owner, String product, String itemsJson) throws Exception {
		mvc.perform(as(put("/api/v1/products/" + product + "/recipe"), owner, "{\"items\":[" + itemsJson + "]}"))
				.andExpect(status().isOk());
	}

}
