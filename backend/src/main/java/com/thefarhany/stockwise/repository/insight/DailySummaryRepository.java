package com.thefarhany.stockwise.repository.insight;

import com.thefarhany.stockwise.model.insight.DailySummary;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailySummaryRepository extends JpaRepository<DailySummary, UUID> {

	Optional<DailySummary> findBySessionId(UUID sessionId);

	/** Ringkasan sesi tertutup dalam rentang tanggal dagang, urut tanggal (ringkasan hanya ada untuk sesi tertutup). */
	@EntityGraph(attributePaths = "session")
	@Query("select s from DailySummary s where s.session.outlet.id = :outletId "
			+ "and s.session.businessDate between :from and :to order by s.session.businessDate")
	List<DailySummary> findInRange(@Param("outletId") UUID outletId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	List<DailySummary> findBySessionIdIn(Collection<UUID> sessionIds);

	/** Mengunci ringkasan supaya jatah insight tidak bisa dipakai dua kali oleh permintaan bersamaan. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from DailySummary s where s.session.id = :sessionId")
	Optional<DailySummary> lockBySessionId(@Param("sessionId") UUID sessionId);

}
