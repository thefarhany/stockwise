package com.thefarhany.stockwise.utils.report;

import com.thefarhany.stockwise.exception.BusinessException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Aturan rentang tanggal untuk laporan rentang dan ekspor: from <= to, paling panjang 366 hari (FR-55). */
public final class ReportRanges {

	public static final int MAX_DAYS = 366;

	private ReportRanges() {
	}

	public static void requireValid(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw invalid("from", "tidak boleh setelah tanggal akhir");
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
			throw invalid("to", "rentang paling panjang " + MAX_DAYS + " hari");
		}
	}

	private static BusinessException invalid(String field, String message) {
		return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
				Map.of("fields", Map.of(field, message)));
	}

}
