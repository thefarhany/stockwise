package com.thefarhany.stockwise.model.catalog;

import com.thefarhany.stockwise.model.Auditable;
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
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "outlet_id")
	private Outlet outlet;

	private String name;

	private String category;

	private long price;

	@Enumerated(EnumType.STRING)
	@Column(name = "stock_mode")
	private StockMode stockMode;

	@Column(name = "sort_order")
	private short sortOrder;

	private boolean active = true;

	protected Product() {
	}

	public Product(Outlet outlet) {
		this.outlet = outlet;
	}

	public UUID getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getName() {
		return name;
	}

	public StockMode getStockMode() {
		return stockMode;
	}

	public void setStockMode(StockMode stockMode) {
		this.stockMode = stockMode;
	}

	public void setSortOrder(short sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public long getPrice() {
		return price;
	}

	public boolean isActive() {
		return active;
	}
}
