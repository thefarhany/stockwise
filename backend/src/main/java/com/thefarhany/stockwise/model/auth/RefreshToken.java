package com.thefarhany.stockwise.model.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private AppUser user;

	@Column(name = "token_hash")
	private String tokenHash;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected RefreshToken() {
	}

	public RefreshToken(AppUser user, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public AppUser getUser() {
		return user;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
