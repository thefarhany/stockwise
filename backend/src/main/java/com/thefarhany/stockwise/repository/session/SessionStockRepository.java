package com.thefarhany.stockwise.repository.session;

import com.thefarhany.stockwise.model.session.SessionStock;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionStockRepository extends JpaRepository<SessionStock, UUID> {

	@EntityGraph(attributePaths = "stockItem")
	List<SessionStock> findBySessionIdOrderByStockItemName(UUID sessionId);

	@EntityGraph(attributePaths = "stockItem")
	List<SessionStock> findBySessionIdInAndStockItemPrimaryTrue(Collection<UUID> sessionIds);

	/** Nilai dan jumlah total per bahan pada sesi terbaru yang memuat bahan itu (dasar harga rata-rata terbaru). */
	@Query("select new com.thefarhany.stockwise.repository.session.ItemCost(ss.stockItem.id, "
			+ "ss.openingValue + ss.addedCost, ss.openingQty + ss.addedQty) from SessionStock ss "
			+ "where ss.session.outlet.id = :outletId and ss.openingQty + ss.addedQty > 0 "
			+ "and ss.session.businessDate = (select max(s2.session.businessDate) from SessionStock s2 "
			+ "where s2.stockItem = ss.stockItem and s2.openingQty + s2.addedQty > 0)")
	List<ItemCost> latestItemCosts(@Param("outletId") UUID outletId);

}
