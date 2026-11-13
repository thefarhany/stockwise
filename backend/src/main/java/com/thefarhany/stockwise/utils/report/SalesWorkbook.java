package com.thefarhany.stockwise.utils.report;

import com.thefarhany.stockwise.model.auth.Language;
import com.thefarhany.stockwise.model.insight.DailySummary;
import com.thefarhany.stockwise.model.sales.PaymentMethod;
import com.thefarhany.stockwise.model.sales.Sale;
import com.thefarhany.stockwise.model.sales.SaleItem;
import com.thefarhany.stockwise.model.sales.SaleStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

/**
 * Menyusun berkas .xlsx ekspor penjualan (FR-55, docs/05 bagian 10): lembar Transaksi, Item, dan Harian.
 * Tanggal = tanggal dagang; jam menurut zona waktu usaha. Lembar Item hanya memuat transaksi selesai, supaya
 * jumlahnya sama dengan omzet di lembar Harian; transaksi batal tetap terlihat di lembar Transaksi.
 * Nama lembar, judul kolom, dan label mengikuti bahasa akun yang mengekspor.
 */
public final class SalesWorkbook {

	private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");
	private static final String DATE = "yyyy-mm-dd";
	private static final String MONEY = "#,##0";

	private SalesWorkbook() {
	}

	public static byte[] write(Language lang, ZoneId zone, List<Sale> sales, List<SaleItem> items,
			List<DailySummary> days) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Workbook wb = new Workbook(out, "Stockwise", "1.0");
			transactions(wb.newWorksheet(t(lang, "Transaksi", "Transactions")), lang, zone, sales);
			items(wb.newWorksheet(t(lang, "Item", "Items")), lang, items);
			daily(wb.newWorksheet(t(lang, "Harian", "Daily")), lang, sales, days);
			wb.finish();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return out.toByteArray();
	}

	private static void transactions(Worksheet ws, Language lang, ZoneId zone, List<Sale> sales) {
		header(ws, t(lang, "Tanggal", "Date"), t(lang, "Jam", "Time"), t(lang, "Nomor struk", "Receipt no."),
				t(lang, "Kasir", "Cashier"), t(lang, "Metode bayar", "Payment method"), "Total",
				t(lang, "Uang diterima", "Cash received"), "Status", t(lang, "Alasan void", "Void reason"));
		int r = 1;
		for (Sale s : sales) {
			date(ws, r, 0, s.getSession().getBusinessDate());
			ws.value(r, 1, HOUR_MINUTE.format(s.getSoldAt().atZone(zone)));
			ws.value(r, 2, s.getReceiptNo());
			ws.value(r, 3, s.getCashier().getName());
			ws.value(r, 4, paymentLabel(lang, s.getPaymentMethod()));
			money(ws, r, 5, s.getTotalAmount());
			if (s.getCashReceived() != null) {
				money(ws, r, 6, s.getCashReceived());
			}
			ws.value(r, 7, s.getStatus() == SaleStatus.VOID ? t(lang, "Batal", "Voided") : t(lang, "Selesai", "Completed"));
			if (s.getVoidReason() != null) {
				ws.value(r, 8, s.getVoidReason());
			}
			r++;
		}
	}

	private static void items(Worksheet ws, Language lang, List<SaleItem> items) {
		header(ws, t(lang, "Tanggal", "Date"), t(lang, "Nomor struk", "Receipt no."), t(lang, "Produk", "Product"),
				t(lang, "Jumlah", "Quantity"), t(lang, "Harga satuan", "Unit price"), "Subtotal");
		int r = 1;
		for (SaleItem i : items) {
			date(ws, r, 0, i.getSale().getSession().getBusinessDate());
			ws.value(r, 1, i.getSale().getReceiptNo());
			ws.value(r, 2, i.getProduct().getName());
			ws.value(r, 3, i.getQuantity());
			money(ws, r, 4, i.getUnitPrice());
			money(ws, r, 5, i.getSubtotal());
			r++;
		}
	}

	private static void daily(Worksheet ws, Language lang, List<Sale> sales, List<DailySummary> days) {
		header(ws, t(lang, "Tanggal", "Date"), t(lang, "Omzet", "Revenue"),
				t(lang, "Biaya terjual (HPP)", "Cost of goods sold"), t(lang, "Biaya terbuang", "Waste cost"),
				t(lang, "Biaya selisih", "Stock difference cost"), t(lang, "Laba", "Profit"),
				t(lang, "Jumlah transaksi", "Transactions"), "Void");
		Map<UUID, Long> completed = sales.stream().filter(s -> s.getStatus() == SaleStatus.COMPLETED)
				.collect(Collectors.groupingBy(s -> s.getSession().getId(), Collectors.counting()));
		int r = 1;
		for (DailySummary d : days) {
			date(ws, r, 0, d.getSession().getBusinessDate());
			money(ws, r, 1, d.getRevenue());
			money(ws, r, 2, d.getCogs());
			money(ws, r, 3, d.getWasteCost());
			money(ws, r, 4, d.getAdjustmentCost());
			money(ws, r, 5, d.getNetProfit());
			ws.value(r, 6, completed.getOrDefault(d.getSession().getId(), 0L));
			ws.value(r, 7, d.getVoidCount());
			r++;
		}
	}

	private static void header(Worksheet ws, String... titles) {
		for (int c = 0; c < titles.length; c++) {
			ws.value(0, c, titles[c]);
			ws.style(0, c).bold().set();
		}
	}

	private static void date(Worksheet ws, int r, int c, LocalDate value) {
		ws.value(r, c, value);
		ws.style(r, c).format(DATE).set();
	}

	private static void money(Worksheet ws, int r, int c, long value) {
		ws.value(r, c, value);
		ws.style(r, c).format(MONEY).set();
	}

	private static String paymentLabel(Language lang, PaymentMethod method) {
		return switch (method) {
			case CASH -> t(lang, "Tunai", "Cash");
			case QRIS -> "QRIS";
			case OTHER -> t(lang, "Lainnya", "Other");
		};
	}

	private static String t(Language lang, String id, String en) {
		return lang == Language.ID ? id : en;
	}

}
