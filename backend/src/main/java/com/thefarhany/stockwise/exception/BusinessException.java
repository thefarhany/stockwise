package com.thefarhany.stockwise.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Pelanggaran aturan bisnis; status dan errorCode langsung jadi respons (mis. 409 SESSION_ALREADY_OPEN). */
public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final Map<String, Object> details;

	public BusinessException(HttpStatus status, String code, String message, Map<String, Object> details) {
		super(message);
		this.status = status;
		this.code = code;
		this.details = details;
	}

	public BusinessException(HttpStatus status, String code, String message) {
		this(status, code, message, null);
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public Map<String, Object> details() {
		return details;
	}

}
