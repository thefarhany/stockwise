package com.thefarhany.stockwise.service.insight;

import com.thefarhany.stockwise.model.session.SessionClosedEvent;
import com.thefarhany.stockwise.model.session.SessionOpenedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Menghubungkan modul session ke insight tanpa session mengenal insight. Dijalankan di dalam transaksi
 * penerbit: bila ringkasan atau saran gagal dibuat, tutup sesi ikut dibatalkan dan bisa diulang.
 */
@Component
class SessionEventsListener {

	private final SummaryService summaryService;
	private final ForecastService forecastService;
	private final RecommendationService recommendationService;

	SessionEventsListener(SummaryService summaryService, ForecastService forecastService,
			RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
		this.summaryService = summaryService;
		this.forecastService = forecastService;
	}

	@EventListener
	void onClosed(SessionClosedEvent event) {
		summaryService.create(event.session());
		forecastService.generate(event.session());
	}

	@EventListener
	void onOpened(SessionOpenedEvent event) {
		recommendationService.applyToSession(event.session());
	}

}
