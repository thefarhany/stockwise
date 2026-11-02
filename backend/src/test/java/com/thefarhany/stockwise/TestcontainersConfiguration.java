package com.thefarhany.stockwise;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.mysql.MySQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysql() {
		return new MySQLContainer("mysql:8.4");
	}

	/** Menggantikan Clock aplikasi di tes supaya jam bisa digeser. */
	@Bean
	@Primary
	TestClock testClock() {
		return new TestClock();
	}

}
