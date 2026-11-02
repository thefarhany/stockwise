package com.thefarhany.stockwise.repository.outlet;

import com.thefarhany.stockwise.dto.outlet.OnboardingCounts;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Query lintas tabel milik satu usaha yang tidak punya entity sendiri: hitungan onboarding dan hapus usaha. */
@Repository
public class OutletDataRepository {

	/** Urutan penting: FK default RESTRICT, jadi anak dihapus sebelum induknya (docs/04 bagian 4.7). */
	private static final List<String> PURGE = List.of(
			"DELETE si FROM sale_item si JOIN sale s ON s.id = si.sale_id WHERE s.outlet_id = UUID_TO_BIN(?)",
			"DELETE FROM sale WHERE outlet_id = UUID_TO_BIN(?)",
			"DELETE x FROM stock_movement x JOIN daily_session d ON d.id = x.session_id WHERE d.outlet_id = UUID_TO_BIN(?)",
			"DELETE x FROM session_stock x JOIN daily_session d ON d.id = x.session_id WHERE d.outlet_id = UUID_TO_BIN(?)",
			"DELETE x FROM daily_summary x JOIN daily_session d ON d.id = x.session_id WHERE d.outlet_id = UUID_TO_BIN(?)",
			"DELETE FROM daily_session WHERE outlet_id = UUID_TO_BIN(?)",
			"DELETE FROM recommendation WHERE outlet_id = UUID_TO_BIN(?)",
			"DELETE FROM forecast WHERE outlet_id = UUID_TO_BIN(?)",
			"DELETE r FROM recipe_item r JOIN product p ON p.id = r.product_id WHERE p.outlet_id = UUID_TO_BIN(?)",
			"DELETE FROM product WHERE outlet_id = UUID_TO_BIN(?)",
			"DELETE FROM stock_item WHERE outlet_id = UUID_TO_BIN(?)",
			"DELETE t FROM refresh_token t JOIN app_user u ON u.id = t.user_id WHERE u.outlet_id = UUID_TO_BIN(?)",
			"DELETE FROM app_user WHERE outlet_id = UUID_TO_BIN(?)",
			"DELETE FROM outlet WHERE id = UUID_TO_BIN(?)");

	private final JdbcTemplate jdbc;

	public OutletDataRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public OnboardingCounts onboardingCounts(UUID outletId) {
		String id = outletId.toString();
		return jdbc.queryForObject("""
				SELECT (SELECT COUNT(*) FROM stock_item WHERE outlet_id = UUID_TO_BIN(?) AND active),
					   (SELECT COUNT(*) FROM product WHERE outlet_id = UUID_TO_BIN(?) AND active),
					   (SELECT COUNT(DISTINCT p.id) FROM product p JOIN recipe_item r ON r.product_id = p.id
						 WHERE p.outlet_id = UUID_TO_BIN(?) AND p.active)""",
				(rs, i) -> new OnboardingCounts(rs.getInt(1), rs.getInt(2), rs.getInt(3)), id, id, id);
	}

	/** Harus dipanggil di dalam satu transaksi. */
	public void purge(UUID outletId) {
		PURGE.forEach(sql -> jdbc.update(sql, outletId.toString()));
	}

}
