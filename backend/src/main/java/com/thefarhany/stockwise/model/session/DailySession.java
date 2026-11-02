package com.thefarhany.stockwise.model.session;

import com.thefarhany.stockwise.model.Auditable;
import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.outlet.Outlet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Satu hari dagang. Sesi CLOSED final: tidak pernah dibuka lagi dan datanya tidak diubah. */
@Entity
@Table(name = "daily_session")
public class DailySession extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "outlet_id")
	private Outlet outlet;

	@Column(name = "business_date")
	private LocalDate businessDate;

	@Enumerated(EnumType.STRING)
	private SessionStatus status = SessionStatus.OPEN;

	@Column(name = "opened_at")
	private Instant openedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "opened_by")
	private AppUser openedBy;

	@Column(name = "closed_at")
	private Instant closedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "closed_by")
	private AppUser closedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "close_type")
	private CloseType closeType;

	@Column(name = "planned_close_time")
	private LocalTime plannedCloseTime;

	private String notes;

	protected DailySession() {
	}

	public DailySession(Outlet outlet, LocalDate businessDate, Instant openedAt, AppUser openedBy,
			LocalTime plannedCloseTime) {
		this.outlet = outlet;
		this.businessDate = businessDate;
		this.openedAt = openedAt;
		this.openedBy = openedBy;
		this.plannedCloseTime = plannedCloseTime;
	}

	public SessionStatus getStatus() {
		return status;
	}

	public Outlet getOutlet() {
		return outlet;
	}

	public UUID getId() {
		return id;
	}

	public void setCloseType(CloseType closeType) {
		this.closeType = closeType;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public LocalTime getPlannedCloseTime() {
		return plannedCloseTime;
	}

	public void setClosedAt(Instant closedAt) {
		this.closedAt = closedAt;
	}

	public void setStatus(SessionStatus status) {
		this.status = status;
	}

	public Instant getOpenedAt() {
		return openedAt;
	}

	public Instant getClosedAt() {
		return closedAt;
	}

	public LocalDate getBusinessDate() {
		return businessDate;
	}

	public void setClosedBy(AppUser closedBy) {
		this.closedBy = closedBy;
	}

	public String getNotes() {
		return notes;
	}

	public CloseType getCloseType() {
		return closeType;
	}
}
