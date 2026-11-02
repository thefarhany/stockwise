package com.thefarhany.stockwise.utils.report;

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
 */
public final class SalesWorkbook {

	private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");
	private static final String DATE = "yyyy-mm-dd";
	private static final String MONEY = "#,##0";

	private SalesWorkbook() {
	}

	public static byte[] write(ZoneId zone, List<Sale> sales, List<SaleItem> items, List<DailySummary> days) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Workbook wb = new Workbook(out, "Stockwise", "1.0");
			transactions(wb.newWorksheet("Transaksi"), zone, sales);
			items(wb.newWorksheet("Item"), items);
			daily(wb.newWorksheet("Harian"), sales, days);
			wb.finish();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return out.toByteArray();
	}

	private static void transactions(Worksheet ws, ZoneId zone, List<Sale> sales) {
		header(ws, "Tanggal", "Jam", "Nomor struk", "Kasir", "Metode bayar", "Total", "Uang diterima", "Status",
				"Alasan void");
		int r = 1;
		for (Sale s : sales) {
			date(ws, r, 0, s.getSession().getBusinessDate());
			ws.value(r, 1, HOUR_MINUTE.format(s.getSoldAt().atZone(zone)));
			ws.value(r, 2, s.getReceiptNo());
			ws.value(r, 3, s.getCashier().getName());
			ws.value(r, 4, paymentLabel(s.getPaymentMethod()));
			money(ws, r, 5, s.getTotalAmount());
			if (s.getCashReceived() != null) {
				money(ws, r, 6, s.getCashReceived());
			}
			ws.value(r, 7, s.getStatus() == SaleStatus.VOID ? "Batal" : "Selesai");
			if (s.getVoidReason() != null) {
				ws.value(r, 8, s.getVoidReason());
			}
			r++;
		}
	}

	private static void items(Worksheet ws, List<SaleItem> items) {
		header(ws, "Tanggal", "Nomor struk", "Produk", "Jumlah", "Harga satuan", "Subtotal");
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

	private static void daily(Worksheet ws, List<Sale> sales, List<DailySummary> days) {
		header(ws, "Tanggal", "Omzet", "Biaya terjual (HPP)", "Biaya terbuang", "Biaya selisih", "Laba",
				"Jumlah transaksi", "Void");
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

	private static String paymentLabel(PaymentMethod method) {
		return switch (method) {
			case CASH -> "Tunai";
			case QRIS -> "QRIS";
			case OTHER -> "Lainnya";
		};
	}

}
