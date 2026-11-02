package com.thefarhany.stockwise;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Daftar kasus API berbentuk tabel: tiap baris = satu permintaan + status HTTP + errorCode yang diharapkan.
 * Semua baris dijalankan dulu, lalu semua yang menyimpang dilaporkan sekaligus (tidak berhenti di yang pertama).
 */
final class ApiCases {

	record Case(String name, MockHttpServletRequestBuilder request, int status, String errorCode) {
	}

	private final MockMvc mvc;
	private final List<Case> cases = new ArrayList<>();

	ApiCases(MockMvc mvc) {
		this.mvc = mvc;
	}

	/** Kasus yang harus ditolak dengan status dan errorCode tertentu. */
	ApiCases fail(String name, int status, String errorCode, MockHttpServletRequestBuilder request) {
		cases.add(new Case(name, request, status, errorCode));
		return this;
	}

	/** Kasus yang harus berhasil dengan status tertentu (errorCode tidak diperiksa). */
	ApiCases ok(String name, int status, MockHttpServletRequestBuilder request) {
		cases.add(new Case(name, request, status, null));
		return this;
	}

	void runAll() throws Exception {
		List<String> deviations = new ArrayList<>();
		for (Case c : cases) {
			var response = mvc.perform(c.request()).andReturn().getResponse();
			String body = response.getContentAsString();
			String code = null;
			try {
				code = JsonPath.read(body, "$.errorCode");
			}
			catch (Exception ignored) {
				// berhasil (tanpa errorCode) atau bukan JSON
			}
			boolean statusOk = response.getStatus() == c.status();
			boolean codeOk = c.errorCode() == null ? code == null : c.errorCode().equals(code);
			if (!statusOk || !codeOk) {
				deviations.add(c.name() + ": harapan " + c.status() + " " + c.errorCode() + ", dapat "
						+ response.getStatus() + " " + code);
			}
		}
		assertThat(deviations).as("%d dari %d kasus menyimpang", deviations.size(), cases.size()).isEmpty();
	}

	// --- pembangun permintaan singkat

	static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String token, String body) {
		if (token != null) {
			b = b.header("Authorization", "Bearer " + token);
		}
		return body == null ? b : b.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	static MockHttpServletRequestBuilder get(String path, String token) {
		return json(MockMvcRequestBuilders.get(path), token, null);
	}

	static MockHttpServletRequestBuilder post(String path, String token, String body) {
		return json(MockMvcRequestBuilders.post(path), token, body);
	}

	static MockHttpServletRequestBuilder put(String path, String token, String body) {
		return json(MockMvcRequestBuilders.put(path), token, body);
	}

	static MockHttpServletRequestBuilder delete(String path, String token, String body) {
		return json(MockMvcRequestBuilders.delete(path), token, body);
	}

}
