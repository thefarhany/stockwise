package com.thefarhany.stockwise;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.json.JsonMapper;

/** Ollama palsu pada port tetap (sesuai application-test.properties): meniru /api/chat, dan merekam permintaannya. */
class FakeOllama implements AutoCloseable {

	static final int PORT = 18099;

	private final JsonMapper mapper = JsonMapper.builder().build();
	private final HttpServer server;
	final AtomicReference<String> lastBody = new AtomicReference<>();
	final AtomicReference<String> lastAuthorization = new AtomicReference<>();
	final AtomicInteger calls = new AtomicInteger();
	private volatile int status = 200;
	private volatile String text = "";
	private volatile long delayMs;

	FakeOllama() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
		server.createContext("/api/chat", exchange -> {
			calls.incrementAndGet();
			lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			try {
				Thread.sleep(delayMs);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			byte[] body = status == 200 ? reply().getBytes(StandardCharsets.UTF_8) : new byte[0];
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
			if (body.length > 0) {
				exchange.getResponseBody().write(body);
			}
			exchange.close();
		});
		server.start();
	}

	/** Model menjawab dengan teks ini (dibungkus format JSON {"text": ...} seperti permintaan format). */
	FakeOllama replyWith(String text) {
		this.status = 200;
		this.text = text;
		return this;
	}

	FakeOllama failWith(int status) {
		this.status = status;
		return this;
	}

	FakeOllama delay(long millis) {
		this.delayMs = millis;
		return this;
	}

	private String reply() {
		String content = mapper.writeValueAsString(Map.of("text", text));
		return mapper.writeValueAsString(Map.of("model", "test-model", "done", true, "message",
				Map.of("role", "assistant", "content", content)));
	}

	@Override
	public void close() {
		server.stop(0);
	}

}
