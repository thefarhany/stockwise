package com.thefarhany.stockwise.service.report;

import com.thefarhany.stockwise.repository.insight.DailySummaryRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.repository.sales.SaleItemRepository;
import com.thefarhany.stockwise.repository.sales.SaleRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import com.thefarhany.stockwise.utils.report.ReportRanges;
import com.thefarhany.stockwise.utils.report.SalesWorkbook;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ekspor penjualan ke Excel (FR-55): hanya sesi tertutup usaha ini, rentang paling panjang 366 hari. */
@Service
public class SalesExportService {

	private final SaleRepository sales;
	private final SaleItemRepository saleItems;
	private final DailySummaryRepository summaries;
	private final OutletRepository outlets;

	public SalesExportService(SaleRepository sales, SaleItemRepository saleItems, DailySummaryRepository summaries,
			OutletRepository outlets) {
		this.sales = sales;
		this.saleItems = saleItems;
		this.summaries = summaries;
		this.outlets = outlets;
	}

	/** Validasi dilakukan sebelum berkas dibuat, jadi error tetap keluar sebagai JSON biasa. */
	@Transactional(readOnly = true)
	public byte[] export(AuthPrincipal me, LocalDate from, LocalDate to) {
		ReportRanges.requireValid(from, to);
		UUID outletId = me.outletId();
		return SalesWorkbook.write(OutletRules.zone(outlets.findById(outletId).orElseThrow()),
				sales.findClosedInRange(outletId, from, to), saleItems.findCompletedInRange(outletId, from, to),
				summaries.findInRange(outletId, from, to));
	}

}
