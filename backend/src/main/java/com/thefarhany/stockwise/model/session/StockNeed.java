package com.thefarhany.stockwise.model.session;

import com.thefarhany.stockwise.model.catalog.StockItem;
import java.math.BigDecimal;

/** Kebutuhan satu bahan untuk satu transaksi (jumlah resep dikali porsi). */
public record StockNeed(StockItem item, BigDecimal quantity) {
}
