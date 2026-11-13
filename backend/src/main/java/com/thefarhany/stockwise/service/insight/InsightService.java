package com.thefarhany.stockwise.service.insight;

import com.thefarhany.stockwise.dto.insight.InsightFacts;
import com.thefarhany.stockwise.dto.insight.InsightResultResponse;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.auth.Language;
import com.thefarhany.stockwise.model.insight.DailySummary;
import com.thefarhany.stockwise.model.insight.InsightStatus;
import com.thefarhany.stockwise.model.insight.Recommendation;
import com.thefarhany.stockwise.model.insight.RecommendationStatus;
import com.thefarhany.stockwise.model.outlet.Currency;
import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.SessionStatus;
import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.repository.auth.AppUserRepository;
import com.thefarhany.stockwise.repository.insight.DailySummaryRepository;
import com.thefarhany.stockwise.repository.insight.RecommendationRepository;
import com.thefarhany.stockwise.repository.session.DailySessionRepository;
import com.thefarhany.stockwise.repository.session.SessionStockRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.utils.insight.InsightTexts;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Insight atas permintaan Owner (FR-54). Model berjalan satu per satu. Panggilan ke model TIDAK boleh menahan
 * transaksi database (bisa puluhan detik), jadi alurnya tiga langkah: pegang antrean, cadangkan satu kesempatan
 * (transaksi pendek), panggil model (tanpa transaksi), lalu simpan hasil (transaksi pendek).
 * Teks dari model dipakai hanya bila semua angkanya ada di fakta; selain itu dipakai template.
 */
@Service
public class InsightService {

	private static final Logger log = LoggerFactory.getLogger(InsightService.class);
	/** Batas pembuatan insight per sesi; satu usaha hanya punya satu sesi per hari, jadi ini juga batas harian. */
	static final int MAX_ATTEMPTS = 3;
	static final String TEMPLATE_MODEL = "template";
	private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

	/** {example} diganti contoh uang dalam mata uang usaha. */
	static final String SYSTEM_PROMPT = """
			Kamu membantu pemilik usaha makanan kecil memahami hasil jualan hari ini.
			Tulis evaluasi penjualan hari ini dan arahan belanja besok dalam bahasa Indonesia sehari-hari, 3 sampai 4 kalimat.
			ATURAN:
			- Hanya pakai angka yang tertulis di FAKTA. Jangan menghitung, membulatkan, atau membuat angka baru.
			- Tulis uang lengkap dengan simbolnya seperti {example}, jangan memakai kata ribu atau juta.
			- Jangan menyebut hal yang tidak ada di FAKTA.
			- Jika closeType adalah AUTO, katakan bahwa sisa stok dihitung otomatis dari stok tercatat.
			Balas hanya dengan JSON: {"text": "..."}""";

	static final String SYSTEM_PROMPT_EN = """
			You help the owner of a small food business understand today's sales.
			Write an evaluation of today's sales and guidance for tomorrow's shopping in plain everyday English, 3 to 4 sentences.
			RULES:
			- Only use numbers written in the FACTS. Do not calculate, round, or create new numbers.
			- Write money in full with its symbol, like {example}; do not use words like thousand or million.
			- Do not mention anything that is not in the FACTS.
			- If closeType is AUTO, say that the leftovers were counted automatically from recorded stock.
			Reply only with JSON: {"text": "..."}""";

	private record Prepared(UUID summaryId, InsightFacts facts, String factsJson) {
	}

	private final DailySessionRepository sessions;
	private final DailySummaryRepository summaries;
	private final SessionStockRepository stocks;
	private final RecommendationRepository recommendations;
	private final AppUserRepository users;
	private final OllamaClient ollama;
	private final ObjectMapper mapper;
	private final Clock clock;
	private final TransactionTemplate tx;
	private final long queueWaitSeconds;
	private final Semaphore gate = new Semaphore(1, true);

	public InsightService(DailySessionRepository sessions, DailySummaryRepository summaries,
			SessionStockRepository stocks, RecommendationRepository recommendations, AppUserRepository users,
			OllamaClient ollama,
			ObjectMapper mapper, Clock clock, PlatformTransactionManager txManager,
			@Value("${stockwise.ollama.queue-wait-seconds}") long queueWaitSeconds) {
		this.sessions = sessions;
		this.summaries = summaries;
		this.stocks = stocks;
		this.recommendations = recommendations;
		this.users = users;
		this.ollama = ollama;
		this.mapper = mapper;
		this.clock = clock;
		this.tx = new TransactionTemplate(txManager);
		this.queueWaitSeconds = queueWaitSeconds;
	}

	public InsightResultResponse generate(AuthPrincipal me, UUID sessionId) {
		acquire();
		try {
			Prepared prepared = tx.execute(status -> reserve(me, sessionId));
			Set<String> allowed = InsightTexts.allowedNumbers(prepared.factsJson());
			Optional<String> generated = ollama.generate(systemPrompt(prepared.facts()),
					(InsightTexts.english(prepared.facts()) ? "FACTS:\n" : "FAKTA:\n") + prepared.factsJson())
					.map(String::trim);
			// null = lolos. Jangan lewat Optional.map: hasil null akan dianggap "tidak ada" dan teks valid ikut terbuang.
			String rejection = generated.isPresent() ? InsightTexts.rejection(generated.get(), allowed,
					InsightTexts.english(prepared.facts()))
					: "model_unavailable";
			Optional<String> fromModel = rejection == null ? generated : Optional.empty();
			if (fromModel.isEmpty() && ollama.isConfigured()) {
				log.warn("insight_fallback_template sessionId={} reason={}", sessionId, rejection);
			}
			String text = fromModel.orElseGet(() -> InsightTexts.template(prepared.facts()));
			String model = fromModel.isPresent() ? ollama.model() : TEMPLATE_MODEL;
			return tx.execute(status -> save(prepared.summaryId(), text, model));
		}
		finally {
			gate.release();
		}
	}

	static String systemPrompt(InsightFacts facts) {
		Currency currency = Currency.valueOf(facts.currency());
		BigDecimal example = new BigDecimal(currency.decimals() == 0 ? "637200" : "637.20");
		return InsightTexts.english(facts)
				? SYSTEM_PROMPT_EN.replace("{example}", InsightTexts.moneyEn(example, currency))
				: SYSTEM_PROMPT.replace("{example}", InsightTexts.money(example, currency));
	}

	private void acquire() {
		try {
			if (!gate.tryAcquire(queueWaitSeconds, TimeUnit.SECONDS)) {
				throw busy();
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw busy();
		}
	}

	/** Memeriksa sesi, mengunci ringkasan, dan memakai satu kesempatan. */
	private Prepared reserve(AuthPrincipal me, UUID sessionId) {
		DailySession session = sessions.findByIdAndOutletId(sessionId, me.outletId()).orElseThrow(
				() -> new BusinessException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Sesi tidak ditemukan."));
		if (session.getStatus() == SessionStatus.OPEN) {
			throw new BusinessException(HttpStatus.CONFLICT, "SESSION_NOT_CLOSED",
					"Insight tersedia setelah sesi ditutup.");
		}
		DailySummary summary = summaries.lockBySessionId(sessionId).orElseThrow(
				() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ringkasan belum tersedia."));
		if (attemptsLeft(summary) == 0) {
			throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "INSIGHT_LIMIT_REACHED",
					"Kesempatan membuat insight untuk sesi ini sudah habis.");
		}
		summary.setInsightAttempts((short) (summary.getInsightAttempts() + 1));
		// Insight ditulis dalam bahasa akun yang memintanya.
		Language language = users.findById(me.userId()).map(AppUser::getLanguage).orElse(Language.ID);
		InsightFacts facts = facts(session, summary, language);
		return new Prepared(summary.getId(), facts, mapper.writeValueAsString(facts));
	}

	private InsightResultResponse save(UUID summaryId, String text, String model) {
		DailySummary summary = summaries.findById(summaryId).orElseThrow();
		summary.setInsightStatus(InsightStatus.READY);
		summary.setInsightText(text);
		summary.setInsightModel(model);
		summary.setInsightGeneratedAt(Instant.now(clock));
		return new InsightResultResponse(summary.getInsightStatus().name(), text, model, attemptsLeft(summary),
				summary.getInsightGeneratedAt());
	}

	/** Angka agregat, nama bahan, dan alasan; tidak ada nama pengguna, email, id, atau nama usaha. */
	private InsightFacts facts(DailySession session, DailySummary s, Language language) {
		List<SessionStock> rows = stocks.findBySessionIdOrderByStockItemName(session.getId());
		InsightFacts.PrimaryFacts primary = SummaryService.primaryItem(s, rows).map(p -> new InsightFacts.PrimaryFacts(
				p.name(), p.soldOutAt() == null ? null
						: HOUR_MINUTE.format(p.soldOutAt().atZone(OutletRules.zone(session.getOutlet()))),
				p.lostSalesEstimate() == null ? 0 : Math.round(p.lostSalesEstimate().doubleValue()))).orElse(null);
		Currency currency = session.getOutlet().getCurrency();
		return new InsightFacts(language.name().toLowerCase(java.util.Locale.ROOT), currency.name(),
				session.getBusinessDate().toString(), session.getCloseType().name(),
				new InsightFacts.Facts(InsightTexts.major(s.getRevenue(), currency),
						InsightTexts.major(s.getCogs(), currency), InsightTexts.major(s.getNetProfit(), currency),
						Math.round(s.getTotalItemsSold().doubleValue()), s.getVoidCount(), primary,
						recommendationFacts(session)));
	}

	private InsightFacts.RecommendationFacts recommendationFacts(DailySession session) {
		return recommendations.findByOutletIdAndTargetDateAndStatus(session.getOutlet().getId(),
				session.getBusinessDate().plusDays(1), RecommendationStatus.ACTIVE).stream()
				.max(Comparator.comparing(Recommendation::getRecommendedQty))
				.map(r -> new InsightFacts.RecommendationFacts(r.getTargetDate().toString(), r.getStockItem().getName(),
						r.getRecommendedQty().doubleValue(), r.getLowerQty().doubleValue(),
						r.getUpperQty().doubleValue(), r.getReasons().stream().map(m -> m.get("text")).toList()))
				.orElse(null);
	}

	/** Sisa kesempatan membuat insight untuk sesi ini. */
	static int attemptsLeft(DailySummary summary) {
		return Math.max(0, MAX_ATTEMPTS - summary.getInsightAttempts());
	}

	private static BusinessException busy() {
		return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "INSIGHT_BUSY",
				"Model sedang dipakai. Coba lagi sebentar lagi.");
	}

}
