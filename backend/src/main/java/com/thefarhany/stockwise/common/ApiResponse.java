package com.thefarhany.stockwise.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/** Amplop seragam untuk semua respons JSON (docs/05-api-contract.md, Format Respons). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
		boolean success,
		String message,
		@JsonInclude(JsonInclude.Include.ALWAYS) T data,
		String errorCode,
		Map<String, Object> details,
		Integer page,
		Integer size,
		Long totalElements,
		Integer totalPages,
		Instant timestamp) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, "OK", data, null, null, null, null, null, null, null);
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(true, message, data, null, null, null, null, null, null, null);
	}

	public static <T> ApiResponse<List<T>> page(Page<T> page) {
		return new ApiResponse<>(true, "OK", page.getContent(), null, null, page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages(), null);
	}

	public static ApiResponse<Void> error(String errorCode, String message, Map<String, Object> details) {
		return new ApiResponse<>(false, message, null, errorCode, details, null, null, null, null,
				Instant.now().truncatedTo(ChronoUnit.SECONDS));
	}

}
