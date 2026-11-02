package com.thefarhany.stockwise.repository.catalog;

import com.thefarhany.stockwise.model.catalog.StockItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

	List<StockItem> findByOutletIdOrderByName(UUID outletId);

	List<StockItem> findByOutletIdAndActiveTrueOrderByName(UUID outletId);

	Optional<StockItem> findByIdAndOutletId(UUID id, UUID outletId);

	List<StockItem> findByIdInAndOutletIdAndActiveTrue(List<UUID> ids, UUID outletId);

	boolean existsByOutletIdAndNameIgnoreCase(UUID outletId, String name);

	boolean existsByOutletIdAndNameIgnoreCaseAndIdNot(UUID outletId, String name, UUID id);

	/** Bahan yang sudah pernah dipakai di sesi tidak boleh ganti satuan (riwayat jadi salah makna). */
	@Query(value = "select count(*) from session_stock where stock_item_id = :id", nativeQuery = true)
	long sessionUsageCount(@Param("id") UUID id);

}
