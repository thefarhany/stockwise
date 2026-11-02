package com.thefarhany.stockwise.exception;

import com.thefarhany.stockwise.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Semua error (termasuk bawaan Spring MVC seperti 404/405) keluar dalam amplop ApiResponse. */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
		return ResponseEntity.status(ex.status()).body(ApiResponse.error(ex.code(), ex.getMessage(), ex.details()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResponse.error("FORBIDDEN", "Tidak punya akses.", null));
	}

	/** Jaring pengaman untuk constraint DB (mis. unique) yang lolos dari pengecekan service. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiResponse<Void>> handleDataConflict() {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error("DATA_CONFLICT", "Data bentrok dengan data yang sudah ada.", null));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
		log.error("unexpected_error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("INTERNAL_ERROR", "Terjadi kesalahan pada server.", null));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, Object> fields = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
		return ResponseEntity.badRequest()
				.body(ApiResponse.error("VALIDATION_ERROR", "Validasi gagal.", Map.of("fields", fields)));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		String code = switch (statusCode.value()) {
			case 400 -> "VALIDATION_ERROR";
			case 404 -> "NOT_FOUND";
			default -> {
				HttpStatus status = HttpStatus.resolve(statusCode.value());
				yield status != null ? status.name() : "ERROR";
			}
		};
		return ResponseEntity.status(statusCode).headers(headers)
				.body(ApiResponse.error(code, ex.getMessage(), null));
	}

}
