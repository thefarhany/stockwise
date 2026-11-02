package com.thefarhany.stockwise.model.catalog;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Jumlah satu stock item per 1 porsi produk, dalam satuan stock item itu. */
@Entity
@Table(name = "recipe_item")
public class RecipeItem {

	@EmbeddedId
	private RecipeItemId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("productId")
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("stockItemId")
	@JoinColumn(name = "stock_item_id")
	private StockItem stockItem;

	private BigDecimal quantity;

	protected RecipeItem() {
	}

	public RecipeItem(RecipeItemId id, Product product, StockItem stockItem, BigDecimal quantity) {
		this.id = id;
		this.product = product;
		this.stockItem = stockItem;
		this.quantity = quantity;
	}

	public Product getProduct() {
		return product;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public StockItem getStockItem() {
		return stockItem;
	}
}
