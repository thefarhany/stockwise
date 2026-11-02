package com.thefarhany.stockwise.repository.auth;

import com.thefarhany.stockwise.model.auth.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** Atomik: dua refresh bersamaan dengan token sama, hanya satu yang dapat 1. */
	@Modifying
	@Query("update RefreshToken t set t.revokedAt = :now "
			+ "where t.tokenHash = :hash and t.revokedAt is null and t.expiresAt > :now")
	int revokeIfActive(@Param("hash") String hash, @Param("now") Instant now);

	@Modifying
	@Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
	int revokeAllOf(@Param("userId") UUID userId, @Param("now") Instant now);

}
