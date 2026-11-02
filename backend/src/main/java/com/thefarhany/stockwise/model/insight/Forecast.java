package com.thefarhany.stockwise.model.insight;

import com.thefarhany.stockwise.model.catalog.Product;
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
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Prediksi permintaan satu produk untuk satu tanggal. */
@Entity
@Table(name = "forecast")
public class Forecast {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "outlet_id")
	private Outlet outlet;

	@Column(name = "target_date")
	private LocalDate targetDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "predicted_qty")
	private BigDecimal predictedQty;

	@Column(name = "lower_qty")
	private BigDecimal lowerQty;

	@Column(name = "upper_qty")
	private BigDecimal upperQty;

	@Enumerated(EnumType.STRING)
	private ForecastMethod method;

	@Column(name = "model_version")
	private String modelVersion;

	/** dataPoints (jumlah sesi dipakai), samples, weekdayBased. */
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> features;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	protected Forecast() {
	}

	public Forecast(Outlet outlet, LocalDate targetDate, Product product, BigDecimal predictedQty,
			BigDecimal lowerQty, BigDecimal upperQty, ForecastMethod method, String modelVersion,
			Map<String, Object> features) {
		this.outlet = outlet;
		this.targetDate = targetDate;
		this.product = product;
		this.predictedQty = predictedQty;
		this.lowerQty = lowerQty;
		this.upperQty = upperQty;
		this.method = method;
		this.modelVersion = modelVersion;
		this.features = features;
	}

	public Map<String, Object> getFeatures() {
		return features;
	}

	public Product getProduct() {
		return product;
	}

	public BigDecimal getLowerQty() {
		return lowerQty;
	}

	public BigDecimal getPredictedQty() {
		return predictedQty;
	}

	public BigDecimal getUpperQty() {
		return upperQty;
	}

	public ForecastMethod getMethod() {
		return method;
	}
}
