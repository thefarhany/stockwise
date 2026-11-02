package com.thefarhany.stockwise.repository.sales;

import com.thefarhany.stockwise.model.sales.SaleItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

	@EntityGraph(attributePaths = "product")
	/** Urut nama produk supaya struk selalu tampil sama setiap kali dicetak. */
	List<SaleItem> findBySaleIdOrderByProductName(UUID saleId);

	/** Jumlah porsi per transaksi, untuk riwayat. */
	@Query("select si.sale.id as saleId, sum(si.quantity) as total from SaleItem si "
			+ "where si.sale.id in :saleIds group by si.sale.id")
	List<ItemTotal> totals(@Param("saleIds") Collection<UUID> saleIds);

	/** Baris transaksi selesai di sesi tertutup dalam rentang tanggal dagang (ekspor Excel); ss.outlet.id demi indeks. */
	@Query("select si from SaleItem si join fetch si.product join fetch si.sale s join fetch s.session ss "
			+ "where s.outlet.id = :outletId and ss.outlet.id = :outletId "
			+ "and ss.status = com.thefarhany.stockwise.model.session.SessionStatus.CLOSED "
			+ "and s.status = com.thefarhany.stockwise.model.sales.SaleStatus.COMPLETED "
			+ "and ss.businessDate between :from and :to order by ss.businessDate, s.receiptNo, si.product.name")
	List<SaleItem> findCompletedInRange(@Param("outletId") UUID outletId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/** Porsi terjual (transaksi selesai, bukan void) dalam satu sesi. */
	@Query("select coalesce(sum(si.quantity), 0) from SaleItem si where si.sale.session.id = :sessionId "
			+ "and si.sale.status = com.thefarhany.stockwise.model.sales.SaleStatus.COMPLETED")
	BigDecimal completedPortions(@Param("sessionId") UUID sessionId);

	/** Permintaan per sesi per produk (hanya transaksi selesai), bahan prediksi. */
	@Query("select si.sale.session.id as sessionId, si.product.id as productId, sum(si.quantity) as total "
			+ "from SaleItem si where si.sale.session.id in :sessionIds "
			+ "and si.sale.status = com.thefarhany.stockwise.model.sales.SaleStatus.COMPLETED "
			+ "group by si.sale.session.id, si.product.id")
	List<ProductDemand> demandBySession(@Param("sessionIds") Collection<UUID> sessionIds);

	/** Penjualan per produk dalam satu sesi (transaksi selesai), terbesar dulu. */
	@Query("select si.product.id as productId, si.product.name as name, sum(si.quantity) as qty, "
			+ "sum(si.subtotal) as revenue from SaleItem si where si.sale.session.id = :sessionId "
			+ "and si.sale.status = com.thefarhany.stockwise.model.sales.SaleStatus.COMPLETED "
			+ "group by si.product.id, si.product.name order by sum(si.subtotal) desc, si.product.name")
	List<ProductSales> productSales(@Param("sessionId") UUID sessionId);

	/** Waktu jual dan jumlah porsi per baris, untuk dikelompokkan per jam menurut zona waktu usaha. */
	@Query("select si.sale.soldAt as soldAt, si.quantity as qty from SaleItem si where si.sale.session.id = :sessionId "
			+ "and si.sale.status = com.thefarhany.stockwise.model.sales.SaleStatus.COMPLETED")
	List<SoldAt> soldTimes(@Param("sessionId") UUID sessionId);

	interface ProductSales {

		UUID getProductId();

		String getName();

		BigDecimal getQty();

		Long getRevenue();

	}

	interface SoldAt {

		java.time.Instant getSoldAt();

		BigDecimal getQty();

	}

	interface ProductDemand {

		UUID getSessionId();

		UUID getProductId();

		BigDecimal getTotal();

	}

	interface ItemTotal {

		UUID getSaleId();

		BigDecimal getTotal();

	}

}
