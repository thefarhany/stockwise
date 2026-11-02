package com.thefarhany.stockwise.service.insight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * ============================= PLACEHOLDER OLLAMA =============================
 * Satu-satunya kelas yang berbicara dengan Ollama. Semua pengaturannya dibaca dari properti
 * `stockwise.ollama.*` (lihat application.properties), yang diisi dari environment atau backend/.env:
 *
 *   OLLAMA_URL      alamat Ollama (default http://localhost:11434; untuk Ollama cloud isi alamat API-nya)
 *   OLLAMA_MODEL    nama model. KOSONG = belum dikonfigurasi, insight langsung memakai teks template
 *   OLLAMA_API_KEY  kunci API (opsional). Bila diisi dikirim sebagai header "Authorization: Bearer ..."
 *
 * Ollama lokal tidak butuh kunci; isi OLLAMA_API_KEY hanya bila Ollama di belakang proxy/cloud yang memintanya.
 * Format panggilan: POST {url}/api/chat, stream=false, format=JSON schema {"text": string}.
 * Mengganti provider berarti mengganti kelas ini saja (docs/03-tech-stack.md bagian 3).
 * ==============================================================================
 *
 * Semua kegagalan (tidak dikonfigurasi, jaringan, waktu habis, jawaban rusak) menghasilkan Optional.empty():
 * pemanggil memakai teks template. Fakta yang dikirim tidak pernah ditulis ke log.
 */
@Component
public class OllamaClient {

	private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

	private static final Map<String, Object> TEXT_SCHEMA = Map.of("type", "object", "properties",
			Map.of("text", Map.of("type", "string")), "required", List.of("text"));

	record Message(String role, String content) {
	}

	record ChatRequest(String model, List<Message> messages, boolean stream, Map<String, Object> format,
			Map<String, Object> options) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ChatResponse(Message message) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Generated(String text) {
	}

	private final RestClient http;
	private final String model;
	private final ObjectMapper mapper;

	public OllamaClient(@Value("${stockwise.ollama.url}") String url, @Value("${stockwise.ollama.model}") String model,
			@Value("${stockwise.ollama.api-key}") String apiKey,
			@Value("${stockwise.ollama.timeout-seconds}") long timeoutSeconds, ObjectMapper mapper) {
		this.model = model == null ? "" : model.trim();
		this.mapper = mapper;
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build());
		factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
		RestClient.Builder builder = RestClient.builder().baseUrl(url).requestFactory(factory);
		if (apiKey != null && !apiKey.isBlank()) {
			builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
		}
		this.http = builder.build();
	}

	public boolean isConfigured() {
		return !model.isEmpty();
	}

	public String model() {
		return model;
	}

	/** Teks dari model, atau kosong bila tidak dikonfigurasi atau panggilan gagal. */
	public Optional<String> generate(String system, String user) {
		if (!isConfigured()) {
			log.info("ollama_skipped reason=model_not_configured");
			return Optional.empty();
		}
		try {
			ChatResponse response = http.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON)
					.body(new ChatRequest(model, List.of(new Message("system", system), new Message("user", user)),
							false, TEXT_SCHEMA, Map.of("temperature", 0.2)))
					.retrieve().body(ChatResponse.class);
			if (response == null || response.message() == null || response.message().content() == null) {
				return Optional.empty();
			}
			return Optional.ofNullable(mapper.readValue(response.message().content(), Generated.class).text());
		}
		catch (RuntimeException ex) {
			log.warn("ollama_failed type={}", ex.getClass().getSimpleName());
			return Optional.empty();
		}
	}

}
