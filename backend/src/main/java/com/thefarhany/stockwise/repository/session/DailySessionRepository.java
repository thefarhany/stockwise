package com.thefarhany.stockwise.repository.session;

import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.SessionStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailySessionRepository extends JpaRepository<DailySession, UUID> {

	@EntityGraph(attributePaths = "outlet")
	Optional<DailySession> findByOutletIdAndStatus(UUID outletId, SessionStatus status);

	@EntityGraph(attributePaths = "outlet")
	Optional<DailySession> findByIdAndOutletId(UUID id, UUID outletId);

	/** Semua sesi yang masih terbuka, untuk pemeriksaan tutup otomatis. */
	@EntityGraph(attributePaths = "outlet")
	List<DailySession> findByStatus(SessionStatus status);

	@EntityGraph(attributePaths = "outlet")
	Optional<DailySession> findByOutletIdAndBusinessDate(UUID outletId, LocalDate businessDate);

	/** Sesi tertutup terbaru lebih dulu, untuk bahan prediksi. */
	List<DailySession> findByOutletIdAndStatusOrderByBusinessDateDesc(UUID outletId, SessionStatus status,
			Pageable pageable);

	Page<DailySession> findByOutletIdAndBusinessDateBetween(UUID outletId, LocalDate from, LocalDate to,
			Pageable pageable);

	boolean existsByOutletIdAndBusinessDate(UUID outletId, LocalDate businessDate);

	Optional<DailySession> findFirstByOutletIdAndStatusOrderByBusinessDateDesc(UUID outletId, SessionStatus status);

	/** Mengunci baris sesi: semua operasi stok antar perangkat berjalan bergiliran. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from DailySession s where s.id = :id and s.outlet.id = :outletId")
	Optional<DailySession> lockByIdAndOutletId(@Param("id") UUID id, @Param("outletId") UUID outletId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from DailySession s where s.id = :id")
	Optional<DailySession> lockById(@Param("id") UUID id);

}
