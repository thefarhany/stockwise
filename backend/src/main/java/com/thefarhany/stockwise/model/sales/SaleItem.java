package com.thefarhany.stockwise.model.sales;

import com.thefarhany.stockwise.model.catalog.Product;
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
import java.util.UUID;

/** Baris transaksi; harga disalin saat transaksi sehingga perubahan harga tidak mengubah riwayat. */
@Entity
@Table(name = "sale_item")
public class SaleItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sale_id")
	private Sale sale;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	private BigDecimal quantity;

	@Column(name = "unit_price")
	private long unitPrice;

	private long subtotal;

	protected SaleItem() {
	}

	public SaleItem(Sale sale, Product product, BigDecimal quantity, long unitPrice, long subtotal) {
		this.sale = sale;
		this.product = product;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.subtotal = subtotal;
	}

	public long getSubtotal() {
		return subtotal;
	}

	public long getUnitPrice() {
		return unitPrice;
	}

	public Product getProduct() {
		return product;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public Sale getSale() {
		return sale;
	}

}
