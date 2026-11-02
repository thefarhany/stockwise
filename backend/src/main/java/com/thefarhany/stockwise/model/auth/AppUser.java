package com.thefarhany.stockwise.model.auth;

import com.thefarhany.stockwise.model.Auditable;
import com.thefarhany.stockwise.model.outlet.Outlet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "outlet_id")
	private Outlet outlet;

	private String name;

	private String email;

	@Column(name = "password_hash")
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	private Role role;

	private boolean active = true;

	protected AppUser() {
	}

	public AppUser(Outlet outlet, String name, String email, String passwordHash, Role role) {
		this.outlet = outlet;
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public boolean isActive() {
		return active;
	}

	public Outlet getOutlet() {
		return outlet;
	}

	public UUID getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getName() {
		return name;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getEmail() {
		return email;
	}

	public Role getRole() {
		return role;
	}
}
