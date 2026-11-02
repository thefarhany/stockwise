package com.thefarhany.stockwise.model.sales;

import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.model.session.DailySession;
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
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** Satu transaksi. Struk tidak disimpan terpisah: dibuat dari data ini. */
@Entity
@Table(name = "sale")
public class Sale {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "outlet_id")
	private Outlet outlet;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id")
	private DailySession session;

	/** Urut per sesi, mulai dari 1. */
	@Column(name = "receipt_no")
	private int receiptNo;

	@Column(name = "sold_at")
	private Instant soldAt;

	@Column(name = "total_amount")
	private long totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method")
	private PaymentMethod paymentMethod;

	/** Hanya untuk CASH; kembalian = cashReceived - totalAmount. */
	@Column(name = "cash_received")
	private Long cashReceived;

	@Enumerated(EnumType.STRING)
	@Column(name = "input_method")
	private InputMethod inputMethod;

	@Enumerated(EnumType.STRING)
	private SaleStatus status = SaleStatus.COMPLETED;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cashier_id")
	private AppUser cashier;

	@Column(name = "void_reason")
	private String voidReason;

	@Column(name = "voided_at")
	private Instant voidedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "voided_by")
	private AppUser voidedBy;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	protected Sale() {
	}

	public Sale(Outlet outlet, DailySession session, int receiptNo, Instant soldAt, long totalAmount, PaymentMethod paymentMethod,
			Long cashReceived, InputMethod inputMethod, AppUser cashier) {
		this.outlet = outlet;
		this.session = session;
		this.receiptNo = receiptNo;
		this.soldAt = soldAt;
		this.totalAmount = totalAmount;
		this.paymentMethod = paymentMethod;
		this.cashReceived = cashReceived;
		this.inputMethod = inputMethod;
		this.cashier = cashier;
	}

	public Instant getSoldAt() {
		return soldAt;
	}

	public Long getCashReceived() {
		return cashReceived;
	}

	public String getVoidReason() {
		return voidReason;
	}

	public Outlet getOutlet() {
		return outlet;
	}

	public SaleStatus getStatus() {
		return status;
	}

	public long getTotalAmount() {
		return totalAmount;
	}

	public UUID getId() {
		return id;
	}

	public void setStatus(SaleStatus status) {
		this.status = status;
	}

	public void setVoidedBy(AppUser voidedBy) {
		this.voidedBy = voidedBy;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setVoidedAt(Instant voidedAt) {
		this.voidedAt = voidedAt;
	}

	public void setVoidReason(String voidReason) {
		this.voidReason = voidReason;
	}

	public AppUser getCashier() {
		return cashier;
	}

	public DailySession getSession() {
		return session;
	}

	public int getReceiptNo() {
		return receiptNo;
	}
}
