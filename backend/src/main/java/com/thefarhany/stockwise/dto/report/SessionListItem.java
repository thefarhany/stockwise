package com.thefarhany.stockwise.dto.report;

import com.thefarhany.stockwise.model.session.CloseType;
import com.thefarhany.stockwise.model.session.SessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Satu baris riwayat sesi. Angka keuangan null untuk sesi yang masih terbuka. */
public record SessionListItem(UUID id, LocalDate businessDate, SessionStatus status, CloseType closeType,
		Long revenue, Long cogs, Long wasteCost, Long netProfit, PrimaryItem primaryItem) {

	/** Item utama: terjual, jam habis (null bila tidak habis), dan saran yang dulu diberikan untuk hari itu. */
	public record PrimaryItem(String name, BigDecimal soldQty, Instant soldOutAt, BigDecimal recommendedQty) {
	}

}
