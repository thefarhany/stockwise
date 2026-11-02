package com.thefarhany.stockwise.insight;

import static org.assertj.core.api.Assertions.assertThat;

import com.thefarhany.stockwise.service.insight.OllamaClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OllamaClientTest {

	@Test
	void blankModelMeansNotConfiguredAndNeverCallsOut() {
		// alamat sengaja tidak bisa dijangkau: bila kode mencoba menelepon, tes ini akan lambat/gagal
		OllamaClient client = new OllamaClient("http://127.0.0.1:1", "  ", "", 1, JsonMapper.builder().build());
		assertThat(client.isConfigured()).isFalse();
		assertThat(client.generate("sistem", "pengguna")).isEmpty();
	}

	@Test
	void unreachableServerFallsBackToEmptyInsteadOfThrowing() {
		OllamaClient client = new OllamaClient("http://127.0.0.1:1", "model-x", "", 1, JsonMapper.builder().build());
		assertThat(client.isConfigured()).isTrue();
		assertThat(client.generate("sistem", "pengguna")).isEmpty();
	}

}
