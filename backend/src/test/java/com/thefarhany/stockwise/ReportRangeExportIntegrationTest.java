package com.thefarhany.stockwise;

import static com.thefarhany.stockwise.ShopFixture.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.thefarhany.stockwise.ShopFixture.Shop;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tiga hari dagang: Senin 2 Nov (3 porsi), Selasa 3 Nov (2 porsi + 1 transaksi dibatalkan), Senin 9 Nov (1 porsi).
 * Harga ayam Rp7.000/pcs dan tepung Rp8/g stabil, jadi HPP per porsi Rp7.800 dan laba per porsi Rp5.200.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ReportRangeExportIntegrationTest {

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

	private static double num(String json, String path) {
		return ((Number) JsonPath.read(json, path)).doubleValue();
	}

	/** Tiga hari dagang di atas; sisa ayam dan tepung dibawa, sambal dibuang. */
	private Shop threeDays() throws Exception {
		Shop s = fixture.create();
		fixture.closedDay(s, clock, 3); // Senin 2 Nov

		clock.set("2026-11-03T04:00:00Z"); // Selasa
		String day2 = fixture.open(s.owner(), s);
		clock.set("2026-11-03T05:00:00Z");
		fixture.sell(s.cashier(), day2, s.goreng(), 2);
		String mistake = fixture.sell(s.cashier(), day2, s.goreng(), 1);
		mvc.perform(as(post("/api/v1/sales/" + mistake + "/void"), s.cashier(), "{\"reason\":\"Salah tekan\"}"))
				.andExpect(status().isOk());
		clock.set("2026-11-03T14:00:00Z");
		fixture.close(s.owner(), day2, s, "15", "3500", "5");

		clock.set("2026-11-09T04:00:00Z"); // Senin berikutnya
		String day3 = fixture.open(s.owner(), s);
		clock.set("2026-11-09T05:00:00Z");
		fixture.sell(s.cashier(), day3, s.goreng(), 1);
		clock.set("2026-11-09T14:00:00Z");
		fixture.close(s.owner(), day3, s, "24", "5400", "5");
		return s;
	}

	@Test
	void rangeReportGroupsByDayAndByIsoWeek() throws Exception {
		Shop s = threeDays();

		String daily = fixture.json(as(get("/api/v1/reports/range?from=2026-11-01&to=2026-11-30"), s.owner(), ""));
		assertThat(JsonPath.<String>read(daily, "$.data.groupBy")).isEqualTo("day");
		assertThat(JsonPath.<List<String>>read(daily, "$.data.points[*].period"))
				.containsExactly("2026-11-02", "2026-11-03", "2026-11-09");
		assertThat(JsonPath.<List<Integer>>read(daily, "$.data.points[*].revenue")).containsExactly(39000, 26000, 13000);
		assertThat(JsonPath.<List<Integer>>read(daily, "$.data.points[*].cogs")).containsExactly(23400, 15600, 7800);
		assertThat(JsonPath.<List<Integer>>read(daily, "$.data.points[*].netProfit")).containsExactly(15600, 10400, 5200);
		assertThat(num(daily, "$.data.points[1].itemsSold")).isEqualTo(2); // yang dibatalkan tidak dihitung
		assertThat(num(daily, "$.data.total.revenue")).isEqualTo(78000);
		assertThat(num(daily, "$.data.total.netProfit")).isEqualTo(31200);
		assertThat(num(daily, "$.data.total.sessions")).isEqualTo(3);

		String weekly = fixture.json(as(get("/api/v1/reports/range?from=2026-11-01&to=2026-11-30&groupBy=week"),
				s.owner(), ""));
		assertThat(JsonPath.<List<String>>read(weekly, "$.data.points[*].period"))
				.containsExactly("2026-11-02", "2026-11-09"); // Senin awal minggu
		assertThat(JsonPath.<List<Integer>>read(weekly, "$.data.points[*].revenue")).containsExactly(65000, 13000);
		assertThat(JsonPath.<List<Integer>>read(weekly, "$.data.points[*].netProfit")).containsExactly(26000, 5200);
		assertThat(JsonPath.<List<Integer>>read(weekly, "$.data.points[*].sessions")).containsExactly(2, 1);
		assertThat(num(weekly, "$.data.points[0].itemsSold")).isEqualTo(5);

		// rentang yang memotong: hanya 3 Nov
		String one = fixture.json(as(get("/api/v1/reports/range?from=2026-11-03&to=2026-11-08"), s.owner(), ""));
		assertThat(JsonPath.<List<Object>>read(one, "$.data.points")).hasSize(1);
	}

	@Test
	void rangeReportValidationRolesAndTenancy() throws Exception {
		Shop s = threeDays();
		Shop other = fixture.create();
		String base = "/api/v1/reports/range";
		mvc.perform(as(get(base + "?from=2026-11-30&to=2026-11-01"), s.owner(), ""))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.from").exists());
		mvc.perform(as(get(base + "?from=2025-01-01&to=2026-11-30"), s.owner(), ""))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.to").exists());
		mvc.perform(as(get(base + "?from=2026-11-01&to=2026-11-30&groupBy=month"), s.owner(), ""))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fields.groupBy").exists());
		mvc.perform(as(get(base + "?from=2026-11-01"), s.owner(), "")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(as(get(base + "?from=2026-11-01&to=2026-11-30"), s.cashier(), "")).andExpect(status().isForbidden());
		mvc.perform(as(get(base + "?from=2026-11-01&to=2026-11-30"), other.owner(), ""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.points.length()").value(0))
				.andExpect(jsonPath("$.data.total.revenue").value(0));
	}

	/**
	 * Isi berkas .xlsx: nama berkas di dalam zip -> teks XML-nya. Dibaca lewat daftar isi zip (central directory),
	 * seperti Excel dan LibreOffice membacanya; FastExcel menulis ukuran entri di akhir (data descriptor).
	 */
	private static Map<String, String> unzip(byte[] xlsx) throws Exception {
		Path tmp = Files.createTempFile("ekspor", ".xlsx");
		try {
			Files.write(tmp, xlsx);
			Map<String, String> parts = new HashMap<>();
			try (ZipFile zip = new ZipFile(tmp.toFile())) {
				for (ZipEntry e : java.util.Collections.list(zip.entries())) {
					parts.put(e.getName(), new String(zip.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8));
				}
			}
			return parts;
		}
		finally {
			Files.deleteIfExists(tmp);
		}
	}

	private static int rows(String sheetXml) {
		return sheetXml.split("<row ", -1).length - 1;
	}

	@Test
	void exportProducesTheThreeSheetWorkbook() throws Exception {
		Shop s = threeDays();
		byte[] file = mvc.perform(as(get("/api/v1/exports/sales?from=2026-11-01&to=2026-11-30"), s.owner(), ""))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition",
						"attachment; filename=\"penjualan-2026-11-01_2026-11-30.xlsx\""))
				.andReturn().getResponse().getContentAsByteArray();

		Map<String, String> xlsx = unzip(file);
		String workbook = xlsx.get("xl/workbook.xml");
		assertThat(workbook).contains("name=\"Transaksi\"", "name=\"Item\"", "name=\"Harian\"");
		// baris = 1 judul + data
		assertThat(rows(xlsx.get("xl/worksheets/sheet1.xml"))).as("Transaksi: 4 transaksi, termasuk yang batal").isEqualTo(5);
		assertThat(rows(xlsx.get("xl/worksheets/sheet2.xml"))).as("Item: hanya transaksi selesai").isEqualTo(4);
		assertThat(rows(xlsx.get("xl/worksheets/sheet3.xml"))).as("Harian: 3 sesi").isEqualTo(4);

		String text = xlsx.getOrDefault("xl/sharedStrings.xml", "") + xlsx.get("xl/worksheets/sheet1.xml")
				+ xlsx.get("xl/worksheets/sheet2.xml");
		assertThat(text).contains("Nomor struk", "Alasan void", "Batal", "Selesai", "Salah tekan", "Tunai", "Siti",
				"Ayam Goreng");
		String harian = xlsx.get("xl/worksheets/sheet3.xml");
		assertThat(harian).contains(">39000<", ">26000<", ">13000<", ">15600<", ">10400<", ">5200<");
	}

	@Test
	void exportNeverContainsAnotherOutletsSales() throws Exception {
		Shop a = threeDays();
		Shop b = fixture.create();
		fixture.closedDay(b, clock, 7); // usaha lain, tanggal dagang yang sama dengan hari pertama usaha a

		byte[] file = mvc.perform(as(get("/api/v1/exports/sales?from=2026-11-01&to=2026-11-30"), a.owner(), ""))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
		Map<String, String> xlsx = unzip(file);
		assertThat(rows(xlsx.get("xl/worksheets/sheet1.xml"))).as("tetap hanya 4 transaksi usaha a").isEqualTo(5);
		assertThat(rows(xlsx.get("xl/worksheets/sheet3.xml"))).isEqualTo(4);
		assertThat(xlsx.get("xl/worksheets/sheet3.xml")).doesNotContain(">91000<"); // omzet 7 porsi usaha b
	}

	@Test
	void exportErrorsStayJsonAndOnlyOwnerCanDownload() throws Exception {
		Shop s = threeDays();
		Shop other = fixture.create();
		mvc.perform(as(get("/api/v1/exports/sales?from=2025-01-01&to=2026-11-30"), s.owner(), ""))
				.andExpect(status().isBadRequest())
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(as(get("/api/v1/exports/sales?from=2026-11-01"), s.owner(), ""))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
		mvc.perform(as(get("/api/v1/exports/sales?from=2026-11-01&to=2026-11-30"), s.cashier(), ""))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

		// usaha lain: berkas sah tetapi hanya berisi judul kolom
		byte[] empty = mvc.perform(as(get("/api/v1/exports/sales?from=2026-11-01&to=2026-11-30"), other.owner(), ""))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
		Map<String, String> xlsx = unzip(empty);
		assertThat(rows(xlsx.get("xl/worksheets/sheet1.xml"))).isEqualTo(1);
		assertThat(rows(xlsx.get("xl/worksheets/sheet3.xml"))).isEqualTo(1);
	}

}
