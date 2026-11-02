package com.thefarhany.stockwise.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Satu sumber waktu, UTC; tes boleh menggantinya. Tanggal dagang memakai Outlet.today(clock). */
@Configuration
class TimeConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
