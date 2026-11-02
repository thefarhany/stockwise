package com.thefarhany.stockwise.model.session;

import com.thefarhany.stockwise.model.catalog.StockItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Ringkasan stok satu bahan dalam satu sesi, termasuk hitungan HPP (docs/04 bagian 3.8). */
@Entity
@Table(name = "session_stock")
public class SessionStock {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id")
	private DailySession session;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "stock_item_id")
	private StockItem stockItem;

	@Column(name = "opening_qty")
	private BigDecimal openingQty = BigDecimal.ZERO;

	@Column(name = "opening_age_days")
	private short openingAgeDays;

	@Column(name = "opening_value")
	private long openingValue;

	@Column(name = "added_qty")
	private BigDecimal addedQty = BigDecimal.ZERO;

	@Column(name = "added_cost")
	private long addedCost;

	@Column(name = "sold_qty")
	private BigDecimal soldQty = BigDecimal.ZERO;

	@Column(name = "adjusted_qty")
	private BigDecimal adjustedQty = BigDecimal.ZERO;

	@Column(name = "closing_qty")
	private BigDecimal closingQty;

	@Column(name = "carried_over_qty")
	private BigDecimal carriedOverQty;

	@Column(name = "discarded_qty")
	private BigDecimal discardedQty;

	@Column(name = "sold_cost")
	private Long soldCost;

	@Column(name = "discarded_cost")
	private Long discardedCost;

	@Column(name = "adjustment_cost")
	private Long adjustmentCost;

	@Column(name = "carried_value")
	private Long carriedValue;

	@Column(name = "sold_out_at")
	private Instant soldOutAt;

	@Column(name = "recommended_qty")
	private BigDecimal recommendedQty;

	protected SessionStock() {
	}

	public SessionStock(DailySession session, StockItem stockItem) {
		this.session = session;
		this.stockItem = stockItem;
	}

	public short getOpeningAgeDays() {
		return openingAgeDays;
	}

	public StockItem getStockItem() {
		return stockItem;
	}

	public BigDecimal getCarriedOverQty() {
		return carriedOverQty;
	}

	public void setOpeningAgeDays(short openingAgeDays) {
		this.openingAgeDays = openingAgeDays;
	}

	public Long getCarriedValue() {
		return carriedValue;
	}

	public BigDecimal getRecommendedQty() {
		return recommendedQty;
	}

	public BigDecimal getClosingQty() {
		return closingQty;
	}

	public BigDecimal getAddedQty() {
		return addedQty;
	}

	public Long getSoldCost() {
		return soldCost;
	}

	public void setOpeningValue(long openingValue) {
		this.openingValue = openingValue;
	}

	public Long getDiscardedCost() {
		return discardedCost;
	}

	public void setOpeningQty(BigDecimal openingQty) {
		this.openingQty = openingQty;
	}

	public BigDecimal getOpeningQty() {
		return openingQty;
	}

	public BigDecimal getSoldQty() {
		return soldQty;
	}

	public void setRecommendedQty(BigDecimal recommendedQty) {
		this.recommendedQty = recommendedQty;
	}

	public BigDecimal getDiscardedQty() {
		return discardedQty;
	}

	public BigDecimal getAdjustedQty() {
		return adjustedQty;
	}

	public Long getAdjustmentCost() {
		return adjustmentCost;
	}

	public Instant getSoldOutAt() {
		return soldOutAt;
	}

	public void setAdjustedQty(BigDecimal adjustedQty) {
		this.adjustedQty = adjustedQty;
	}

	public void setClosingQty(BigDecimal closingQty) {
		this.closingQty = closingQty;
	}

	public void setCarriedOverQty(BigDecimal carriedOverQty) {
		this.carriedOverQty = carriedOverQty;
	}

	public void setSoldQty(BigDecimal soldQty) {
		this.soldQty = soldQty;
	}

	public void setCarriedValue(Long carriedValue) {
		this.carriedValue = carriedValue;
	}

	public void setDiscardedQty(BigDecimal discardedQty) {
		this.discardedQty = discardedQty;
	}

	public void setAdjustmentCost(Long adjustmentCost) {
		this.adjustmentCost = adjustmentCost;
	}

	public long getAddedCost() {
		return addedCost;
	}

	public void setSoldCost(Long soldCost) {
		this.soldCost = soldCost;
	}

	public DailySession getSession() {
		return session;
	}

	public void setSoldOutAt(Instant soldOutAt) {
		this.soldOutAt = soldOutAt;
	}

	public void setDiscardedCost(Long discardedCost) {
		this.discardedCost = discardedCost;
	}

	public long getOpeningValue() {
		return openingValue;
	}

	public void setAddedQty(BigDecimal addedQty) {
		this.addedQty = addedQty;
	}

	public void setAddedCost(long addedCost) {
		this.addedCost = addedCost;
	}
}
