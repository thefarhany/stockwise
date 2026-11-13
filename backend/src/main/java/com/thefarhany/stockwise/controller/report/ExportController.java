package com.thefarhany.stockwise.controller.report;

import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.report.SalesExportService;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Satu-satunya endpoint yang tidak memakai ApiResponse saat sukses: isinya berkas, bukan JSON (docs/05). */
@RestController
@PreAuthorize("hasRole('OWNER')")
class ExportController {

	static final MediaType XLSX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final SalesExportService service;

	ExportController(SalesExportService service) {
		this.service = service;
	}

	@GetMapping("/api/v1/exports/sales")
	ResponseEntity<byte[]> sales(@AuthenticationPrincipal AuthPrincipal me, @RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		SalesExportService.Export export = service.export(me, from, to);
		return ResponseEntity.ok().contentType(XLSX)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(export.filename()).build().toString())
				.body(export.file());
	}

}
