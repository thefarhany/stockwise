package com.thefarhany.stockwise.repository.insight;

import com.thefarhany.stockwise.model.insight.Forecast;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForecastRepository extends JpaRepository<Forecast, UUID> {

	@EntityGraph(attributePaths = "product")
	List<Forecast> findByOutletIdAndTargetDate(UUID outletId, LocalDate targetDate);

	@Modifying(flushAutomatically = true, clearAutomatically = false)
	@Query("delete from Forecast f where f.outlet.id = :outletId and f.targetDate = :targetDate")
	void deleteFor(@Param("outletId") UUID outletId, @Param("targetDate") LocalDate targetDate);

}
