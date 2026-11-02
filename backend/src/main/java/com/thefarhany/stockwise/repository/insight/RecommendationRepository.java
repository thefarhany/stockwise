package com.thefarhany.stockwise.repository.insight;

import com.thefarhany.stockwise.model.insight.Recommendation;
import com.thefarhany.stockwise.model.insight.RecommendationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

	@EntityGraph(attributePaths = "stockItem")
	List<Recommendation> findByOutletIdAndTargetDateAndStatus(UUID outletId, LocalDate targetDate,
			RecommendationStatus status);

	boolean existsByOutletIdAndTargetDateAndStatus(UUID outletId, LocalDate targetDate, RecommendationStatus status);

	/** Saran lama untuk tanggal yang sama diganti, bukan dihapus. */
	@Modifying(flushAutomatically = true, clearAutomatically = false)
	@Query("update Recommendation r set r.status = com.thefarhany.stockwise.model.insight.RecommendationStatus.SUPERSEDED "
			+ "where r.outlet.id = :outletId and r.targetDate = :targetDate "
			+ "and r.status = com.thefarhany.stockwise.model.insight.RecommendationStatus.ACTIVE")
	void supersede(@Param("outletId") UUID outletId, @Param("targetDate") LocalDate targetDate);

}
