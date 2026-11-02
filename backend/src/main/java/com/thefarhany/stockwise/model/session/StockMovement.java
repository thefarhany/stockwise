package com.thefarhany.stockwise.model.session;

import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.catalog.StockItem;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Buku besar perubahan stok: hanya ditambah, tidak pernah diubah. Jumlah bertanda (masuk +, keluar -). */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id")
	private DailySession session;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "stock_item_id")
	private StockItem stockItem;

	@Enumerated(EnumType.STRING)
	private MovementType type;

	private BigDecimal quantity;

	@Column(name = "unit_cost")
	private BigDecimal unitCost;

	@Column(name = "reference_id")
	private UUID referenceId;

	private String reason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private AppUser createdBy;

	@Column(name = "created_at")
	private Instant createdAt;

	protected StockMovement() {
	}

	/** createdBy null = tindakan sistem (tutup otomatis). */
	public StockMovement(DailySession session, StockItem stockItem, MovementType type, BigDecimal quantity,
			BigDecimal unitCost, String reason, AppUser createdBy, Instant createdAt) {
		this.session = session;
		this.stockItem = stockItem;
		this.type = type;
		this.quantity = quantity;
		this.unitCost = unitCost;
		this.reason = reason;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
	}

	public StockItem getStockItem() {
		return stockItem;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setReferenceId(UUID referenceId) {
		this.referenceId = referenceId;
	}
}
