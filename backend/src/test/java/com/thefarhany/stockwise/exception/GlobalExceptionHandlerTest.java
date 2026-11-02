package com.thefarhany.stockwise.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thefarhany.stockwise.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

	record Body(@NotBlank String name) {}

	@RestController
	static class Probe {

		@GetMapping("/business")
		void business() {
			throw new BusinessException(HttpStatus.CONFLICT, "SESSION_ALREADY_OPEN", "Masih ada sesi terbuka.",
					Map.of("openSessionId", "s-1"));
		}

		@GetMapping("/boom")
		void boom() {
			throw new IllegalStateException("password=rahasia jdbc:mysql://db:3306");
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new DataIntegrityViolationException("Duplicate entry 'x' for key 'uq_internal_index'");
		}

		@GetMapping("/ok")
		ApiResponse<Map<String, String>> ok() {
			return ApiResponse.ok(Map.of("id", "x"));
		}

		@GetMapping("/empty")
		ApiResponse<Void> empty() {
			return ApiResponse.ok(null);
		}

		@GetMapping("/paged")
		ApiResponse<List<String>> paged() {
			return ApiResponse.page(new PageImpl<>(List.of("a"), PageRequest.of(0, 20), 1));
		}

		@PostMapping("/validate")
		void validate(@Valid @RequestBody Body body) {
		}

	}

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Probe())
			.setControllerAdvice(new GlobalExceptionHandler()).build();

	@Test
	void successWrapsData() throws Exception {
		mvc.perform(get("/ok"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("OK"))
				.andExpect(jsonPath("$.data.id").value("x"))
				.andExpect(jsonPath("$.errorCode").doesNotExist())
				.andExpect(jsonPath("$.page").doesNotExist());
	}

	@Test
	void emptyDataIsExplicitNull() throws Exception {
		mvc.perform(get("/empty"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value((Object) null))
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasKey("data")));
	}

	@Test
	void pagedAddsPageFields() throws Exception {
		mvc.perform(get("/paged"))
				.andExpect(jsonPath("$.data[0]").value("a"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));
	}

	@Test
	void businessExceptionKeepsStatusCodeAndDetails() throws Exception {
		mvc.perform(get("/business"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("SESSION_ALREADY_OPEN"))
				.andExpect(jsonPath("$.details.openSessionId").value("s-1"))
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasKey("data")))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void validationErrorListsFields() throws Exception {
		mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.details.fields.name").exists());
	}

	@Test
	void unknownPathUsesSameFormat() throws Exception {
		mvc.perform(get("/nope"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

	@Test
	void unexpectedErrorIsGenericAndLeaksNothing() throws Exception {
		mvc.perform(get("/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
				.andExpect(content().string(not(containsString("rahasia"))))
				.andExpect(content().string(not(containsString("jdbc"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	@Test
	void integrityViolationBecomesConflictWithoutSqlDetails() throws Exception {
		mvc.perform(get("/conflict"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("DATA_CONFLICT"))
				.andExpect(content().string(not(containsString("uq_internal_index"))));
	}

	@Test
	void unsupportedMethodAndMediaTypeUseSameFormat() throws Exception {
		mvc.perform(post("/ok"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
		mvc.perform(post("/validate").contentType(MediaType.TEXT_PLAIN).content("x"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
		mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{rusak"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

}
