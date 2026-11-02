package com.thefarhany.stockwise.dto.session;

import com.thefarhany.stockwise.model.session.CloseType;
import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.SessionStatus;
import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.utils.session.SessionRules;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SessionResponse(UUID id, LocalDate businessDate, SessionStatus status, Instant openedAt,
		Instant closedAt, CloseType closeType, LocalTime plannedCloseTime, Instant autoCloseAt, String notes,
		List<SessionStockResponse> stock) {

	public static SessionResponse from(DailySession s, List<SessionStock> rows, boolean showCost) {
		return new SessionResponse(s.getId(), s.getBusinessDate(), s.getStatus(), s.getOpenedAt(), s.getClosedAt(),
				s.getCloseType(), s.getPlannedCloseTime(), SessionRules.autoCloseAt(s), s.getNotes(),
				rows.stream().map(r -> SessionStockResponse.from(r, showCost)).toList());
	}

}
