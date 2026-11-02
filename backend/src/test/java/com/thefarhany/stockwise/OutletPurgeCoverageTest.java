package com.thefarhany.stockwise;

import static org.assertj.core.api.Assertions.assertThat;

import com.thefarhany.stockwise.repository.outlet.OutletDataRepository;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Penjaga: hapus usaha (FR-49) harus mencakup SEMUA tabel. Tabel baru yang belum dimasukkan ke
 * OutletDataRepository.PURGE membuat tes ini gagal, sebelum hapus usaha gagal karena FK atau menyisakan data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OutletPurgeCoverageTest {

	@Autowired
	JdbcTemplate jdbc;

	/** Tabel yang dihapus PURGE, sesuai urutan eksekusi (tabel pertama setelah FROM di tiap pernyataan). */
	@SuppressWarnings("unchecked")
	private static List<String> purgeOrder() {
		List<String> statements = (List<String>) ReflectionTestUtils.getField(OutletDataRepository.class, "PURGE");
		return statements.stream().map(sql -> {
			Matcher m = Pattern.compile("FROM (\\w+)").matcher(sql);
			m.find();
			return m.group(1);
		}).toList();
	}

	@Test
	void everyTableIsCoveredByTheOutletPurge() {
		Set<String> inDatabase = new TreeSet<>(jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class)
				.stream().map(String::toLowerCase).toList());
		inDatabase.remove("flyway_schema_history");

		assertThat(purgeOrder()).as("tabel di PURGE").containsExactlyInAnyOrderElementsOf(inDatabase);
	}

	@Test
	void purgeOrderNeverDeletesAParentBeforeItsChildren() {
		// setiap tabel anak (punya FK ke tabel lain yang juga dihapus) harus muncul lebih dulu daripada induknya
		List<String> order = purgeOrder();
		List<String[]> fks = jdbc.query("""
				SELECT table_name, referenced_table_name FROM information_schema.key_column_usage
				WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL""",
				(rs, i) -> new String[] {rs.getString(1).toLowerCase(), rs.getString(2).toLowerCase()});
		for (String[] fk : fks) {
			if (fk[0].equals(fk[1])) {
				continue;
			}
			assertThat(order.indexOf(fk[0])).as(fk[0] + " dihapus sebelum " + fk[1]).isLessThan(order.indexOf(fk[1]));
		}
	}

}
