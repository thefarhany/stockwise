package com.thefarhany.stockwise;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockwiseApplication {

	// Semua waktu disimpan UTC (docs/04). Jam buka/tutup usaha adalah jam dinding lokal (kolom TIME): bila zona JVM
	// bukan UTC, Hibernate (jdbc.time_zone=UTC) menggesernya saat disimpan, mis. 06:00 WIB tersimpan 23:00 dan
	// ditolak ck_outlet_hours. Dengan JVM berzona UTC tidak ada pergeseran, dan hasilnya tidak bergantung pada mesin.
	// Berlaku untuk jalan normal (main). Di tes, zona UTC diatur lewat argLine surefire di pom.xml karena driver MySQL
	// sudah memuat zona lama sebelum kelas ini diinisialisasi. Di server, jalankan juga dengan -Duser.timezone=UTC.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(StockwiseApplication.class, args);
	}

}
