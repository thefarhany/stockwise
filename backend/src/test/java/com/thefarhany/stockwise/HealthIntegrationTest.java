package com.thefarhany.stockwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class HealthIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ApplicationContext context;

	@Test
	void healthIsPublicAndOtherActuatorEndpointsAreNot() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
		mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
	}

	@Test
	void simulationClockExistsOnlyUnderTheSimulationProfile() {
		// jam yang bisa digeser tidak boleh ikut aktif di profil default/prod
		assertThat(context.containsBean("simulationClock")).isFalse();
		assertThat(context.containsBean("simulationClockController")).isFalse();
	}

}
