package com.thefarhany.stockwise.model.outlet;

import com.thefarhany.stockwise.model.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outlet")
public class Outlet extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String name;

	private String timezone = "Asia/Jakarta";

	@Enumerated(EnumType.STRING)
	private Currency currency = Currency.IDR;

	@Column(name = "open_time")
	private LocalTime openTime;

	@Column(name = "close_time")
	private LocalTime closeTime;

	private BigDecimal latitude;

	private BigDecimal longitude;

	@Embedded
	private OutletAddress address;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payday_days")
	private List<Integer> paydayDays = List.of(25, 1);

	@Column(name = "recommendation_buffer_pct")
	private BigDecimal recommendationBufferPct = new BigDecimal("5.00");

	/** Target margin saran harga jual, persen dari harga jual. */
	@Column(name = "target_margin_pct")
	private BigDecimal targetMarginPct = new BigDecimal("40.00");

	protected Outlet() {
	}

	public Outlet(String name) {
		this.name = name;
	}

	public LocalTime getOpenTime() {
		return openTime;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTimezone() {
		return timezone;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public LocalTime getCloseTime() {
		return closeTime;
	}

	public List<Integer> getPaydayDays() {
		return paydayDays;
	}

	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}

	public void setOpenTime(LocalTime openTime) {
		this.openTime = openTime;
	}

	public void setCloseTime(LocalTime closeTime) {
		this.closeTime = closeTime;
	}

	public OutletAddress getAddress() {
		return address;
	}

	public void setAddress(OutletAddress address) {
		this.address = address;
	}

	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getRecommendationBufferPct() {
		return recommendationBufferPct;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRecommendationBufferPct(BigDecimal recommendationBufferPct) {
		this.recommendationBufferPct = recommendationBufferPct;
	}

	public BigDecimal getTargetMarginPct() {
		return targetMarginPct;
	}

	public void setTargetMarginPct(BigDecimal targetMarginPct) {
		this.targetMarginPct = targetMarginPct;
	}

	public void setPaydayDays(List<Integer> paydayDays) {
		this.paydayDays = paydayDays;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}
}
