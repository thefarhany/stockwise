package com.thefarhany.stockwise.utils.insight;

import com.thefarhany.stockwise.dto.insight.InsightFacts;
import com.thefarhany.stockwise.model.outlet.Currency;
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
	/** Ribuan bertitik, boleh dengan desimal berkoma: 1.620.000 atau 1.234,50. */
	private static final Pattern THOUSANDS = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?");
	/** Ribuan berkoma, boleh dengan desimal bertitik: 1,620,000 atau 1,234.50. */
	private static final Pattern THOUSANDS_EN = Pattern.compile("\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?");
	public static final int MAX_LENGTH = 1200;

	private InsightTexts() {
	}

	/** Uang utuh gaya Indonesia dengan simbol mata uang: Rp637.200, $1.234,50. */
	public static String money(BigDecimal amount, Currency currency) {
		DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(ID));
		df.setMinimumFractionDigits(currency.decimals());
		df.setMaximumFractionDigits(currency.decimals());
		return currency.symbol() + df.format(amount);
	}

	/** Satuan terkecil (sen, rupiah) ke satuan utuh: 123450 sen -> 1234.50. */
	public static BigDecimal major(long minor, Currency currency) {
		return BigDecimal.valueOf(minor, currency.decimals());
	}

	/** Uang utuh gaya English: Rp637,200, $1,234.50. */
	public static String moneyEn(BigDecimal amount, Currency currency) {
		DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
		df.setMinimumFractionDigits(currency.decimals());
		df.setMaximumFractionDigits(currency.decimals());
		return currency.symbol() + df.format(amount);
	}

	public static boolean english(InsightFacts f) {
		return "en".equals(f.language());
	}

	/** Teks cadangan dalam bahasa facts.language. Semua angka berasal dari facts, jadi selalu lolos validasi angka. */
	public static String template(InsightFacts f) {
		return english(f) ? templateEn(f) : templateId(f);
	}

	private static String templateId(InsightFacts f) {
		InsightFacts.Facts x = f.facts();
		StringBuilder sb = new StringBuilder();
		String day = dayName(f.date());
		Currency currency = Currency.valueOf(f.currency());
		sb.append(day).append(" laku ").append(x.itemsSold()).append(" porsi dengan omzet ")
				.append(money(x.revenue(), currency)).append(" dan laba ").append(money(x.netProfit(), currency))
				.append(". ");
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

	private static String templateEn(InsightFacts f) {
		InsightFacts.Facts x = f.facts();
		StringBuilder sb = new StringBuilder();
		Currency currency = Currency.valueOf(f.currency());
		sb.append("On ").append(dayNameEn(f.date())).append(" you sold ").append(x.itemsSold())
				.append(" portions, with revenue of ").append(moneyEn(x.revenue(), currency)).append(" and profit of ")
				.append(moneyEn(x.netProfit(), currency)).append(". ");
		if (x.voidCount() > 0) {
			sb.append(x.voidCount()).append(x.voidCount() == 1 ? " transaction was" : " transactions were")
					.append(" voided. ");
		}
		InsightFacts.PrimaryFacts p = x.primaryItem();
		if (p != null && p.soldOutAt() != null) {
			sb.append(p.name()).append(" sold out at ").append(p.soldOutAt());
			if (p.lostSales() > 0) {
				sb.append(", so about ").append(p.lostSales()).append(" customers were missed");
			}
			sb.append(". ");
		}
		InsightFacts.RecommendationFacts r = x.recommendation();
		if (r != null) {
			sb.append("For ").append(dayNameEn(r.date())).append(", prepare about ").append(qtyEn(r.qty())).append(' ')
					.append(r.item()).append(" (range ").append(qtyEn(r.lower())).append(" to ").append(qtyEn(r.upper()))
					.append("). ");
		}
		if ("AUTO".equals(f.closeType())) {
			sb.append("Today's leftovers were counted automatically from recorded stock, so count them yourself at the "
					+ "next closing for better suggestions.");
		}
		return sb.toString().trim();
	}

	/** 115 untuk 115.0, 4,5 untuk 4.5 (jumlah bahan). */
	static String qty(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
	}

	/** 115 untuk 115.0, 4.5 untuk 4.5. */
	static String qtyEn(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
	}

	/** English, mis. "Saturday"; tanggal tidak terbaca menghasilkan "that day". */
	public static String dayNameEn(String isoDate) {
		try {
			return LocalDate.parse(isoDate).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US);
		}
		catch (RuntimeException ex) {
			return "that day";
		}
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
		return numbersIn(factsJson, false);
	}

	/**
	 * Alasan teks ditolak, atau null bila boleh dipakai. Untuk angka yang tidak dikenal hanya angkanya yang
	 * disebut (bukan teksnya), jadi aman ditulis ke log. english = teks memakai koma ribuan dan titik desimal.
	 */
	public static String rejection(String text, Set<String> allowed) {
		return rejection(text, allowed, false);
	}

	public static String rejection(String text, Set<String> allowed, boolean english) {
		if (text == null || text.isBlank()) {
			return "empty";
		}
		if (text.length() > MAX_LENGTH) {
			return "too_long";
		}
		Set<String> unknown = new java.util.TreeSet<>(numbersIn(text, english));
		unknown.removeAll(allowed);
		return unknown.isEmpty() ? null : "unknown_numbers=" + unknown;
	}

	private static Set<String> numbersIn(String text, boolean english) {
		Set<String> found = new HashSet<>();
		Matcher m = NUMBER.matcher(text);
		while (m.find()) {
			found.add(english ? canonicalEn(m.group()) : canonical(m.group()));
		}
		return found;
	}

	/** English: 1,234.50 -> 1234.5, 18:40 -> 18.4. */
	static String canonicalEn(String token) {
		String t = token.replace(':', '.');
		if (THOUSANDS_EN.matcher(t).matches()) {
			t = t.replace(",", "");
		}
		try {
			return new BigDecimal(t).stripTrailingZeros().toPlainString();
		}
		catch (NumberFormatException ex) {
			return token;
		}
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
