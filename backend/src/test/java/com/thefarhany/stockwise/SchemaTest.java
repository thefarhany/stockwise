package com.thefarhany.stockwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Memastikan V1 benar-benar membuat FK (REFERENCES inline diabaikan MySQL) dan aturan inti di level DB. */
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SchemaTest {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void foreignKeysExist() {
		Integer fks = jdbc.queryForObject("""
				SELECT COUNT(*) FROM information_schema.referential_constraints
				WHERE constraint_schema = DATABASE()""", Integer.class);
		assertThat(fks).isEqualTo(25);
	}

	@Test
	void onlyOneOpenSessionPerOutletAndOneSessionPerDate() {
		jdbc.update("INSERT INTO outlet (id, name) VALUES (UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'T')");
		jdbc.update("""
				INSERT INTO app_user (id, outlet_id, name, email, password_hash, role) VALUES
				(UUID_TO_BIN('00000000-0000-0000-0000-0000000000a1'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'),
				 'O', 'o@t.id', 'x', 'OWNER')""");
		String insert = """
				INSERT INTO daily_session (outlet_id, business_date, status, opened_at, opened_by, planned_close_time)
				VALUES (UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), ?, ?, NOW(6),
				        UUID_TO_BIN('00000000-0000-0000-0000-0000000000a1'), '21:00:00')""";
		jdbc.update(insert, "2026-11-01", "OPEN");
		assertThatThrownBy(() -> jdbc.update(insert, "2026-11-02", "OPEN")).isInstanceOf(Exception.class);
		jdbc.update("UPDATE daily_session SET status = 'CLOSED'");
		jdbc.update(insert, "2026-11-02", "OPEN");
		assertThatThrownBy(() -> jdbc.update(insert, "2026-11-02", "CLOSED")).isInstanceOf(Exception.class);
	}

	@Test
	void closeTimeMustBeAfterOpenTime() {
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO outlet (name, open_time, close_time) VALUES ('X', '21:00:00', '08:00:00')"))
				.isInstanceOf(Exception.class);
	}

}
