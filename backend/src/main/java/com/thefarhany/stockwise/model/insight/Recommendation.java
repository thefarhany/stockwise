package com.thefarhany.stockwise.model.insight;

import com.thefarhany.stockwise.model.catalog.StockItem;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Saran stok satu bahan utama untuk satu tanggal (prediksi dikali resep, dikurangi sisa layak, ditambah buffer). */
@Entity
@Table(name = "recommendation")
public class Recommendation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "outlet_id")
	private Outlet outlet;

	@Column(name = "target_date")
	private LocalDate targetDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "stock_item_id")
	private StockItem stockItem;

	@Column(name = "required_qty")
	private BigDecimal requiredQty;

	@Column(name = "carry_over_qty")
	private BigDecimal carryOverQty;

	@Column(name = "recommended_qty")
	private BigDecimal recommendedQty;

	@Column(name = "lower_qty")
	private BigDecimal lowerQty;

	@Column(name = "upper_qty")
	private BigDecimal upperQty;

	@Column(name = "estimated_cost")
	private long estimatedCost;

	/** Alasan terstruktur: [{"code": "...", "text": "..."}]. Inilah yang diverbalkan ke LLM. */
	@JdbcTypeCode(SqlTypes.JSON)
	private List<Map<String, String>> reasons;

	@Enumerated(EnumType.STRING)
	private RecommendationStatus status = RecommendationStatus.ACTIVE;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	protected Recommendation() {
	}

	public Recommendation(Outlet outlet, LocalDate targetDate, StockItem stockItem, BigDecimal requiredQty,
			BigDecimal carryOverQty, BigDecimal recommendedQty, BigDecimal lowerQty, BigDecimal upperQty,
			long estimatedCost, List<Map<String, String>> reasons) {
		this.outlet = outlet;
		this.targetDate = targetDate;
		this.stockItem = stockItem;
		this.requiredQty = requiredQty;
		this.carryOverQty = carryOverQty;
		this.recommendedQty = recommendedQty;
		this.lowerQty = lowerQty;
		this.upperQty = upperQty;
		this.estimatedCost = estimatedCost;
		this.reasons = reasons;
	}

	public StockItem getStockItem() {
		return stockItem;
	}

	public BigDecimal getRecommendedQty() {
		return recommendedQty;
	}

	public BigDecimal getUpperQty() {
		return upperQty;
	}

	public BigDecimal getRequiredQty() {
		return requiredQty;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public LocalDate getTargetDate() {
		return targetDate;
	}

	public long getEstimatedCost() {
		return estimatedCost;
	}

	public List<Map<String, String>> getReasons() {
		return reasons;
	}

	public BigDecimal getCarryOverQty() {
		return carryOverQty;
	}

	public BigDecimal getLowerQty() {
		return lowerQty;
	}
}
