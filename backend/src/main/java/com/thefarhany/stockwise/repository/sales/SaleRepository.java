package com.thefarhany.stockwise.repository.sales;

import com.thefarhany.stockwise.model.sales.PaymentMethod;
import com.thefarhany.stockwise.model.sales.Sale;
import com.thefarhany.stockwise.model.sales.SaleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

	@EntityGraph(attributePaths = { "session", "cashier", "outlet" })
	Optional<Sale> findByIdAndOutletId(UUID id, UUID outletId);

	/** Sesi pemilik transaksi, dibaca ringan dulu supaya baris sesi bisa dikunci sebelum transaksi dimuat. */
	@Query("select s.session.id from Sale s where s.id = :id and s.outlet.id = :outletId")
	Optional<UUID> findSessionId(@Param("id") UUID id, @Param("outletId") UUID outletId);

	@EntityGraph(attributePaths = "cashier")
	Page<Sale> findBySessionId(UUID sessionId, Pageable pageable);

	@Query("select coalesce(sum(s.totalAmount), 0) from Sale s where s.session.id = :sessionId "
			+ "and s.status = com.thefarhany.stockwise.model.sales.SaleStatus.COMPLETED")
	long completedRevenue(@Param("sessionId") UUID sessionId);

	/** Omzet per metode bayar (transaksi selesai). */
	@Query("select s.paymentMethod as method, sum(s.totalAmount) as total from Sale s where s.session.id = :sessionId "
			+ "and s.status = com.thefarhany.stockwise.model.sales.SaleStatus.COMPLETED group by s.paymentMethod")
	List<PaymentTotal> paymentTotals(@Param("sessionId") UUID sessionId);

	interface PaymentTotal {

		PaymentMethod getMethod();

		Long getTotal();

	}

	long countBySessionIdAndStatus(UUID sessionId, SaleStatus status);

	/**
	 * Transaksi di sesi tertutup dalam rentang tanggal dagang, urut tanggal lalu nomor struk (ekspor Excel).
	 * ss.outlet.id sengaja ikut disaring: tanpanya indeks (outlet_id, business_date) tidak terpakai dan seluruh
	 * tabel sesi (semua usaha) dibaca.
	 */
	@Query("select s from Sale s join fetch s.cashier join fetch s.session ss where s.outlet.id = :outletId "
			+ "and ss.outlet.id = :outletId and ss.status = com.thefarhany.stockwise.model.session.SessionStatus.CLOSED "
			+ "and ss.businessDate between :from and :to order by ss.businessDate, s.receiptNo")
	List<Sale> findClosedInRange(@Param("outletId") UUID outletId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/** Aman dipakai hanya setelah baris sesi dikunci. */
	@Query("select coalesce(max(s.receiptNo), 0) from Sale s where s.session.id = :sessionId")
	int maxReceiptNo(@Param("sessionId") UUID sessionId);

}
