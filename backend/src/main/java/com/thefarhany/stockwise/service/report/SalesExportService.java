package com.thefarhany.stockwise.service.report;

import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.auth.Language;
import com.thefarhany.stockwise.repository.auth.AppUserRepository;
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

	/** language menentukan nama lembar, judul kolom, dan nama berkas. */
	public record Export(String filename, byte[] file) {
	}

	private final SaleRepository sales;
	private final SaleItemRepository saleItems;
	private final DailySummaryRepository summaries;
	private final OutletRepository outlets;
	private final AppUserRepository users;

	public SalesExportService(SaleRepository sales, SaleItemRepository saleItems, DailySummaryRepository summaries,
			OutletRepository outlets, AppUserRepository users) {
		this.sales = sales;
		this.saleItems = saleItems;
		this.summaries = summaries;
		this.outlets = outlets;
		this.users = users;
	}

	/** Validasi dilakukan sebelum berkas dibuat, jadi error tetap keluar sebagai JSON biasa. */
	@Transactional(readOnly = true)
	public Export export(AuthPrincipal me, LocalDate from, LocalDate to) {
		ReportRanges.requireValid(from, to);
		UUID outletId = me.outletId();
		Language lang = users.findById(me.userId()).map(AppUser::getLanguage).orElse(Language.ID);
		byte[] file = SalesWorkbook.write(lang, OutletRules.zone(outlets.findById(outletId).orElseThrow()),
				sales.findClosedInRange(outletId, from, to), saleItems.findCompletedInRange(outletId, from, to),
				summaries.findInRange(outletId, from, to));
		String prefix = lang == Language.ID ? "penjualan-" : "sales-";
		return new Export(prefix + from + "_" + to + ".xlsx", file);
	}

}
