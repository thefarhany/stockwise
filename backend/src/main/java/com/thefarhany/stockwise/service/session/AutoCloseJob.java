package com.thefarhany.stockwise.service.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tiap menit menutup sesi yang melewati jam tutup + 30 menit. Aman dijalankan ulang (cek status di dalam kunci). */
@Component
@ConditionalOnProperty(name = "stockwise.auto-close.enabled", havingValue = "true", matchIfMissing = true)
class AutoCloseJob {

	private static final Logger log = LoggerFactory.getLogger(AutoCloseJob.class);

	private final SessionService sessionService;

	AutoCloseJob(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	// shortcut: tanpa ShedLock, jadi hanya satu instance API yang boleh menjalankan ini; bila nanti lebih dari satu, tambah kunci terjadwal
	@Scheduled(fixedDelayString = "${stockwise.auto-close.interval-ms:60000}")
	void run() {
		int closed = sessionService.autoCloseDue();
		if (closed > 0) {
			log.info("auto_close closed={}", closed);
		}
	}

}
