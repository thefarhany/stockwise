package com.thefarhany.stockwise.repository.catalog;

import com.thefarhany.stockwise.model.catalog.RecipeItem;
import com.thefarhany.stockwise.model.catalog.RecipeItemId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, RecipeItemId> {

	@EntityGraph(attributePaths = "stockItem")
	List<RecipeItem> findByProductId(UUID productId);

	@EntityGraph(attributePaths = "stockItem")
	List<RecipeItem> findByProductIdIn(Collection<UUID> productIds);

	@EntityGraph(attributePaths = "stockItem")
	List<RecipeItem> findByProductOutletId(UUID outletId);

	/** Bulk delete langsung ke DB, supaya insert resep baru dengan kunci sama tidak bentrok urutan flush. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from RecipeItem r where r.product.id = :productId")
	void deleteByProductId(@Param("productId") UUID productId);

	@Query("select distinct r.product.name from RecipeItem r "
			+ "where r.stockItem.id = :stockItemId and r.product.active = true order by r.product.name")
	List<String> activeProductNamesUsing(@Param("stockItemId") UUID stockItemId);

}
