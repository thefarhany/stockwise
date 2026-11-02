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
@Table(name = "stock_item")
public class StockItem extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "outlet_id")
	private Outlet outlet;

	private String name;

	@Enumerated(EnumType.STRING)
	private StockKind kind;

	@Enumerated(EnumType.STRING)
	private StockUnit unit;

	/** Harga perkiraan per satuan; dipakai sebelum ada belanja. */
	@Column(name = "unit_cost")
	private long unitCost;

	@Column(name = "shelf_life_days")
	private short shelfLifeDays;

	@Column(name = "carry_over_allowed")
	private boolean carryOverAllowed;

	@Column(name = "is_primary")
	private boolean primary;

	private boolean active = true;

	protected StockItem() {
	}

	public StockItem(Outlet outlet) {
		this.outlet = outlet;
	}

	public StockUnit getUnit() {
		return unit;
	}

	public UUID getId() {
		return id;
	}

	public StockKind getKind() {
		return kind;
	}

	public String getName() {
		return name;
	}

	public boolean isPrimary() {
		return primary;
	}

	public short getShelfLifeDays() {
		return shelfLifeDays;
	}

	public void setUnit(StockUnit unit) {
		this.unit = unit;
	}

	public void setUnitCost(long unitCost) {
		this.unitCost = unitCost;
	}

	public void setKind(StockKind kind) {
		this.kind = kind;
	}

	public void setCarryOverAllowed(boolean carryOverAllowed) {
		this.carryOverAllowed = carryOverAllowed;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isCarryOverAllowed() {
		return carryOverAllowed;
	}

	public long getUnitCost() {
		return unitCost;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setShelfLifeDays(short shelfLifeDays) {
		this.shelfLifeDays = shelfLifeDays;
	}
}
