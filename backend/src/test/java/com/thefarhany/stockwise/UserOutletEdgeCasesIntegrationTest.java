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

/** Akun staf dan profil usaha: alur sukses, input salah, batas nilai, hak akses Cashier, isolasi usaha. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UserOutletEdgeCasesIntegrationTest {

	private static final String PROFILE = "\"name\":\"Warung\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\","
			+ "\"paydayDays\":[25,1],\"recommendationBufferPct\":5";

	@Autowired
	MockMvc mvc;

	private ShopFixture fixture;
	private Shop s;
	private String ownerEmail;
	private String cashierId;

	@BeforeEach
	void setUp() throws Exception {
		fixture = new ShopFixture(mvc);
		s = fixture.create();
		ownerEmail = JsonPath.read(fixture.json(get("/api/v1/me", s.owner())), "$.data.user.email");
		List<String> ids = JsonPath.read(fixture.json(get("/api/v1/users", s.owner())), "$.data[?(@.role=='CASHIER')].id");
		cashierId = ids.get(0);
	}

	private static String newUser(String name, String email, String password) {
		return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private static String mail() {
		return "u-" + UUID.randomUUID() + "@x.id";
	}

	@Test
	void ownerManagesStaffAccounts() throws Exception {
		String email = mail();
		new ApiCases(mvc)
				.fail("sandi 7 karakter", 400, "VALIDATION_ERROR", post("/api/v1/users", s.owner(), newUser("X", mail(), "1234567")))
				.fail("nama kosong", 400, "VALIDATION_ERROR", post("/api/v1/users", s.owner(), newUser("", mail(), "kasir12345")))
				.fail("email tidak valid", 400, "VALIDATION_ERROR", post("/api/v1/users", s.owner(), newUser("X", "bukan-email", "kasir12345")))
				.fail("email sama dengan Owner (beda huruf)", 409, "EMAIL_TAKEN", post("/api/v1/users", s.owner(), newUser("X", ownerEmail.toUpperCase(), "kasir12345")))
				.ok("buat staf baru", 201, post("/api/v1/users", s.owner(), newUser("Dewi", email, "kasir12345")))
				.fail("buat staf dengan email yang baru dipakai", 409, "EMAIL_TAKEN", post("/api/v1/users", s.owner(), newUser("Dewi2", email, "kasir12345")))
				.ok("ubah nama staf", 200, put("/api/v1/users/" + cashierId, s.owner(), "{\"name\":\"Siti Aminah\"}"))
				.ok("body kosong = tidak ada yang berubah", 200, put("/api/v1/users/" + cashierId, s.owner(), "{}"))
				.fail("nama hanya spasi", 400, "VALIDATION_ERROR", put("/api/v1/users/" + cashierId, s.owner(), "{\"name\":\"   \"}"))
				.fail("nama 101 karakter", 400, "VALIDATION_ERROR", put("/api/v1/users/" + cashierId, s.owner(), "{\"name\":\"" + "a".repeat(101) + "\"}"))
				.fail("id tidak ada", 404, "NOT_FOUND", put("/api/v1/users/" + UUID.randomUUID(), s.owner(), "{\"name\":\"Z\"}"))
				.fail("id bukan UUID", 400, "VALIDATION_ERROR", put("/api/v1/users/abc", s.owner(), "{\"name\":\"Z\"}"))
				.runAll();
		String list = fixture.json(get("/api/v1/users", s.owner()));
		assertThat(JsonPath.<List<String>>read(list, "$.data[*].name")).contains("Siti Aminah", "Dewi");
		assertThat(JsonPath.<List<String>>read(list, "$.data[*].role")).containsOnly("OWNER", "CASHIER");
	}

	@Test
	void deactivatedStaffCanBeReactivatedButOwnerCannotLockHimselfOut() throws Exception {
		String ownerId = JsonPath.read(fixture.json(get("/api/v1/me", s.owner())), "$.data.user.id");
		List<String> cashierEmails = JsonPath.read(fixture.json(get("/api/v1/users", s.owner())), "$.data[?(@.role=='CASHIER')].email");
		String cashierEmail = cashierEmails.get(0);
		String login = "{\"email\":\"" + cashierEmail + "\",\"password\":\"kasir12345\"}";
		new ApiCases(mvc)
				.fail("Owner menonaktifkan dirinya", 400, "VALIDATION_ERROR", put("/api/v1/users/" + ownerId, s.owner(), "{\"active\":false}"))
				.ok("nonaktifkan kasir", 200, put("/api/v1/users/" + cashierId, s.owner(), "{\"active\":false}"))
				.fail("kasir nonaktif tidak bisa login", 401, "UNAUTHORIZED", post("/api/v1/auth/login", null, login))
				.ok("aktifkan kembali", 200, put("/api/v1/users/" + cashierId, s.owner(), "{\"active\":true}"))
				.ok("kasir bisa login lagi", 200, post("/api/v1/auth/login", null, login))
				.runAll();
	}

	@Test
	void cashierCannotManageStaffOrOutlet() throws Exception {
		new ApiCases(mvc)
				.fail("lihat daftar staf", 403, "FORBIDDEN", get("/api/v1/users", s.cashier()))
				.fail("buat staf", 403, "FORBIDDEN", post("/api/v1/users", s.cashier(), newUser("X", mail(), "kasir12345")))
				.fail("ubah staf", 403, "FORBIDDEN", put("/api/v1/users/" + cashierId, s.cashier(), "{\"name\":\"Z\"}"))
				.fail("lihat profil usaha", 403, "FORBIDDEN", get("/api/v1/outlet", s.cashier()))
				.fail("ubah profil usaha", 403, "FORBIDDEN", put("/api/v1/outlet", s.cashier(), "{" + PROFILE + "}"))
				.fail("hapus usaha", 403, "FORBIDDEN", delete("/api/v1/outlet", s.cashier(), "{\"confirmName\":\"Geprek Pak Budi\"}"))
				.ok("profil sendiri tetap boleh", 200, get("/api/v1/me", s.cashier()))
				.runAll();
	}

	@Test
	void staffOfAnotherOutletIsUntouchable() throws Exception {
		Shop other = fixture.create();
		new ApiCases(mvc)
				.fail("ubah staf usaha lain", 404, "NOT_FOUND", put("/api/v1/users/" + cashierId, other.owner(), "{\"name\":\"Hack\"}"))
				.fail("nonaktifkan staf usaha lain", 404, "NOT_FOUND", put("/api/v1/users/" + cashierId, other.owner(), "{\"active\":false}"))
				.runAll();
		assertThat(JsonPath.<List<String>>read(fixture.json(get("/api/v1/users", other.owner())), "$.data[*].name"))
				.doesNotContain("Hack").hasSize(2);
	}

	@Test
	void outletProfileValidationAndBoundaries() throws Exception {
		String tail = ",\"paydayDays\":[25,1],\"recommendationBufferPct\":5";
		new ApiCases(mvc)
				.ok("profil lengkap", 200, put("/api/v1/outlet", s.owner(), "{" + PROFILE + "}"))
				.fail("nama kosong", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\"" + tail + "}"))
				.fail("nama 101 karakter", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"" + "a".repeat(101) + "\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\"" + tail + "}"))
				.fail("jam tutup sama dengan jam buka", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"11:00\",\"closeTime\":\"11:00\"" + tail + "}"))
				.fail("jam tutup sebelum jam buka (lewat tengah malam belum didukung)", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"18:00\",\"closeTime\":\"02:00\"" + tail + "}"))
				.fail("format jam salah", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"sebelas\",\"closeTime\":\"21:00\"" + tail + "}"))
				.fail("jam buka kosong", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"closeTime\":\"21:00\"" + tail + "}"))
				.fail("garis lintang 90,01", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{" + PROFILE + ",\"latitude\":90.01}"))
				.fail("garis bujur -180,01", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{" + PROFILE + ",\"longitude\":-180.01}"))
				.ok("lintang dan bujur tepat di batas", 200, put("/api/v1/outlet", s.owner(), "{" + PROFILE + ",\"latitude\":-90,\"longitude\":180}"))
				.fail("tanggal gajian 0", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\",\"paydayDays\":[0],\"recommendationBufferPct\":5}"))
				.fail("tanggal gajian 32", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\",\"paydayDays\":[32],\"recommendationBufferPct\":5}"))
				.ok("tanpa tanggal gajian", 200, put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\",\"paydayDays\":[],\"recommendationBufferPct\":5}"))
				.fail("buffer -0,01", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\",\"paydayDays\":[1],\"recommendationBufferPct\":-0.01}"))
				.fail("buffer 100,01", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\",\"paydayDays\":[1],\"recommendationBufferPct\":100.01}"))
				.ok("buffer 0 dan 100 sah", 200, put("/api/v1/outlet", s.owner(), "{\"name\":\"W\",\"openTime\":\"11:00\",\"closeTime\":\"21:00\",\"paydayDays\":[1],\"recommendationBufferPct\":100}"))
				.fail("target margin 91", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{" + PROFILE + ",\"targetMarginPct\":91}"))
				.fail("target margin negatif", 400, "VALIDATION_ERROR", put("/api/v1/outlet", s.owner(), "{" + PROFILE + ",\"targetMarginPct\":-1}"))
				.ok("target margin 0 dan 90 sah", 200, put("/api/v1/outlet", s.owner(), "{" + PROFILE + ",\"targetMarginPct\":90}"))
				.runAll();
		String outlet = fixture.json(get("/api/v1/outlet", s.owner()));
		assertThat(JsonPath.<Number>read(outlet, "$.data.targetMarginPct").doubleValue()).isEqualTo(90.0);
		assertThat(JsonPath.<Boolean>read(outlet, "$.data.setupCompleted")).isTrue();
	}

	@Test
	void deletingTheOutletNeedsExactConfirmationAndCutsAllAccess() throws Exception {
		new ApiCases(mvc)
				.fail("tanpa body", 400, "VALIDATION_ERROR", delete("/api/v1/outlet", s.owner(), null))
				.fail("body kosong", 400, "VALIDATION_ERROR", delete("/api/v1/outlet", s.owner(), "{}"))
				.fail("nama kosong", 400, "VALIDATION_ERROR", delete("/api/v1/outlet", s.owner(), "{\"confirmName\":\"\"}"))
				.fail("beda huruf besar/kecil", 400, "VALIDATION_ERROR", delete("/api/v1/outlet", s.owner(), "{\"confirmName\":\"geprek pak budi\"}"))
				.fail("ada spasi di ujung", 400, "VALIDATION_ERROR", delete("/api/v1/outlet", s.owner(), "{\"confirmName\":\"Geprek Pak Budi \"}"))
				.ok("nama persis sama", 200, delete("/api/v1/outlet", s.owner(), "{\"confirmName\":\"Geprek Pak Budi\"}"))
				.fail("usaha sudah hilang untuk pemiliknya", 401, "UNAUTHORIZED", get("/api/v1/outlet", s.owner()))
				.fail("usaha sudah hilang untuk kasirnya", 401, "UNAUTHORIZED", get("/api/v1/me", s.cashier()))
				.runAll();
	}

}
