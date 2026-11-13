package com.thefarhany.stockwise.service.auth;

import com.thefarhany.stockwise.dto.auth.CreateUserRequest;
import com.thefarhany.stockwise.dto.auth.UpdateUserRequest;
import com.thefarhany.stockwise.dto.auth.UserInfo;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.auth.Language;
import com.thefarhany.stockwise.model.auth.Role;
import com.thefarhany.stockwise.repository.auth.AppUserRepository;
import com.thefarhany.stockwise.repository.auth.RefreshTokenRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Akun staf. Semua query dibatasi outletId dari token. */
@Service
public class UserService {

	private final AppUserRepository users;
	private final OutletRepository outlets;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder encoder;
	private final Clock clock;

	public UserService(AppUserRepository users, OutletRepository outlets, RefreshTokenRepository refreshTokens,
			PasswordEncoder encoder, Clock clock) {
		this.clock = clock;
		this.users = users;
		this.outlets = outlets;
		this.refreshTokens = refreshTokens;
		this.encoder = encoder;
	}

	@Transactional(readOnly = true)
	public List<UserInfo> list(UUID outletId) {
		return users.findByOutletIdOrderByName(outletId).stream().map(UserInfo::from).toList();
	}

	@Transactional
	public UserInfo createCashier(UUID outletId, CreateUserRequest r) {
		String email = AuthService.normalize(r.email());
		if (users.existsByEmail(email)) {
			throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email sudah terdaftar.");
		}
		AppUser cashier = users.save(new AppUser(outlets.getReferenceById(outletId), r.name().trim(), email,
				encoder.encode(r.password()), Role.CASHIER));
		cashier.setLanguage(Language.ID); // kasir mulai dalam Bahasa Indonesia; bisa diganti sendiri setelah login
		return UserInfo.from(cashier);
	}

	@Transactional
	public UserInfo update(AuthPrincipal me, UUID id, UpdateUserRequest r) {
		AppUser user = users.findByIdAndOutletId(id, me.outletId())
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Akun tidak ditemukan."));
		if (Boolean.FALSE.equals(r.active()) && user.getId().equals(me.userId())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
					"Akun Owner sendiri tidak bisa dinonaktifkan.");
		}
		if (r.name() != null) {
			user.setName(r.name().trim());
		}
		if (r.active() != null) {
			user.setActive(r.active());
		}
		if (Boolean.FALSE.equals(r.active())) {
			refreshTokens.revokeAllOf(user.getId(), Instant.now(clock));
		}
		return UserInfo.from(user);
	}

}
