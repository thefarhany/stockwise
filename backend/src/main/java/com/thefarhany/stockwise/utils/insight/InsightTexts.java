package com.thefarhany.stockwise.utils.insight;

import com.thefarhany.stockwise.dto.insight.InsightFacts;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Teks cadangan (template) dan pemeriksa angka untuk teks dari model. */
public final class InsightTexts {

	private static final Locale ID = Locale.of("id", "ID");
	private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,:]\\d+)*");
	private static final Pattern THOUSANDS = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+");
	public static final int MAX_LENGTH = 1200;

	private InsightTexts() {
	}

	public static String rupiah(long amount) {
		DecimalFormat df = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
		return "Rp" + df.format(amount).replace(',', '.');
	}

	/** Semua angka di sini berasal dari facts, jadi selalu lolos validasi angka. */
	public static String template(InsightFacts f) {
		InsightFacts.Facts x = f.facts();
		StringBuilder sb = new StringBuilder();
		String day = dayName(f.date());
		sb.append(day).append(" laku ").append(x.itemsSold()).append(" porsi dengan omzet ")
				.append(rupiah(x.revenue())).append(" dan laba ").append(rupiah(x.netProfit())).append(". ");
		if (x.voidCount() > 0) {
			sb.append("Ada ").append(x.voidCount()).append(" transaksi yang dibatalkan. ");
		}
		InsightFacts.PrimaryFacts p = x.primaryItem();
		if (p != null && p.soldOutAt() != null) {
			sb.append(p.name()).append(" habis jam ").append(p.soldOutAt().replace(':', '.'));
			if (p.lostSales() > 0) {
				sb.append(" sehingga sekitar ").append(p.lostSales()).append(" pembeli terlewat");
			}
			sb.append(". ");
		}
		InsightFacts.RecommendationFacts r = x.recommendation();
		if (r != null) {
			sb.append("Untuk ").append(dayName(r.date())).append(" siapkan sekitar ").append(qty(r.qty())).append(' ')
					.append(r.item()).append(" (kisaran ").append(qty(r.lower())).append(" sampai ").append(qty(r.upper()))
					.append("). ");
		}
		if ("AUTO".equals(f.closeType())) {
			sb.append("Sisa stok hari ini dihitung otomatis dari stok tercatat, jadi hitung langsung saat tutup lapak "
					+ "berikutnya agar saran makin tepat.");
		}
		return sb.toString().trim();
	}

	/** 115 untuk 115.0, 4,5 untuk 4.5 (jumlah bahan). */
	static String qty(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
	}

	/** Bahasa Indonesia, mis. "Sabtu"; tanggal tidak terbaca menghasilkan "Hari itu". */
	public static String dayName(String isoDate) {
		try {
			return LocalDate.parse(isoDate).getDayOfWeek().getDisplayName(TextStyle.FULL, ID);
		}
		catch (RuntimeException ex) {
			return "Hari itu";
		}
	}

	/** Angka dalam bentuk kanonik dari semua angka, jam, dan tanggal yang tertulis di teks sumber (JSON fakta). */
	public static Set<String> allowedNumbers(String factsJson) {
		return numbersIn(factsJson);
	}

	/**
	 * Alasan teks ditolak, atau null bila boleh dipakai. Untuk angka yang tidak dikenal hanya angkanya yang
	 * disebut (bukan teksnya), jadi aman ditulis ke log.
	 */
	public static String rejection(String text, Set<String> allowed) {
		if (text == null || text.isBlank()) {
			return "empty";
		}
		if (text.length() > MAX_LENGTH) {
			return "too_long";
		}
		Set<String> unknown = new java.util.TreeSet<>(numbersIn(text));
		unknown.removeAll(allowed);
		return unknown.isEmpty() ? null : "unknown_numbers=" + unknown;
	}

	private static Set<String> numbersIn(String text) {
		Set<String> found = new HashSet<>();
		Matcher m = NUMBER.matcher(text);
		while (m.find()) {
			found.add(canonical(m.group()));
		}
		return found;
	}

	static String canonical(String token) {
		String t = token.replace(':', '.');
		if (THOUSANDS.matcher(t).matches()) {
			t = t.replace(".", "");
		}
		t = t.replace(',', '.');
		try {
			return new BigDecimal(t).stripTrailingZeros().toPlainString();
		}
		catch (NumberFormatException ex) {
			return token; // bentuk aneh (mis. 1.2.3): dicocokkan apa adanya, hampir pasti tidak ada di fakta
		}
	}

}
