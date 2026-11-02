package com.thefarhany.stockwise.model.insight;

import com.thefarhany.stockwise.model.Auditable;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Angka hasil satu sesi (final sejak sesi ditutup) dan insight atas permintaan. */
@Entity
@Table(name = "daily_summary")
public class DailySummary extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id")
	private DailySession session;

	private long revenue;

	private long cogs;

	@Column(name = "waste_cost")
	private long wasteCost;

	@Column(name = "adjustment_cost")
	private long adjustmentCost;

	@Column(name = "carried_value")
	private long carriedValue;

	@Column(name = "expense_total")
	private long expenseTotal;

	@Column(name = "net_profit")
	private long netProfit;

	@Column(name = "total_items_sold")
	private BigDecimal totalItemsSold;

	@Column(name = "void_count")
	private int voidCount;

	/** {"stock_item_id": "...", "qty": 3.0, "sold_qty": 10.0}; null bila item utama tidak habis. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "lost_sales_estimate")
	private Map<String, Object> lostSalesEstimate;

	@Enumerated(EnumType.STRING)
	@Column(name = "insight_status")
	private InsightStatus insightStatus = InsightStatus.NONE;

	@Column(name = "insight_text")
	private String insightText;

	@Column(name = "insight_model")
	private String insightModel;

	@Column(name = "insight_attempts")
	private short insightAttempts;

	@Column(name = "insight_generated_at")
	private Instant insightGeneratedAt;

	protected DailySummary() {
	}

	public DailySummary(DailySession session, long revenue, long cogs, long wasteCost, long adjustmentCost,
			long carriedValue, long netProfit, BigDecimal totalItemsSold, int voidCount,
			Map<String, Object> lostSalesEstimate) {
		this.session = session;
		this.revenue = revenue;
		this.cogs = cogs;
		this.wasteCost = wasteCost;
		this.adjustmentCost = adjustmentCost;
		this.carriedValue = carriedValue;
		this.netProfit = netProfit;
		this.totalItemsSold = totalItemsSold;
		this.voidCount = voidCount;
		this.lostSalesEstimate = lostSalesEstimate;
	}

	public UUID getId() {
		return id;
	}

	public Map<String, Object> getLostSalesEstimate() {
		return lostSalesEstimate;
	}

	public void setInsightText(String insightText) {
		this.insightText = insightText;
	}

	public String getInsightModel() {
		return insightModel;
	}

	public BigDecimal getTotalItemsSold() {
		return totalItemsSold;
	}

	public void setInsightStatus(InsightStatus insightStatus) {
		this.insightStatus = insightStatus;
	}

	public long getCogs() {
		return cogs;
	}

	public void setInsightGeneratedAt(Instant insightGeneratedAt) {
		this.insightGeneratedAt = insightGeneratedAt;
	}

	public long getNetProfit() {
		return netProfit;
	}

	public long getExpenseTotal() {
		return expenseTotal;
	}

	public void setInsightModel(String insightModel) {
		this.insightModel = insightModel;
	}

	public InsightStatus getInsightStatus() {
		return insightStatus;
	}

	public long getCarriedValue() {
		return carriedValue;
	}

	public short getInsightAttempts() {
		return insightAttempts;
	}

	public String getInsightText() {
		return insightText;
	}

	public long getAdjustmentCost() {
		return adjustmentCost;
	}

	public int getVoidCount() {
		return voidCount;
	}

	public long getRevenue() {
		return revenue;
	}

	public Instant getInsightGeneratedAt() {
		return insightGeneratedAt;
	}

	public void setInsightAttempts(short insightAttempts) {
		this.insightAttempts = insightAttempts;
	}

	public long getWasteCost() {
		return wasteCost;
	}

	public DailySession getSession() {
		return session;
	}
}
