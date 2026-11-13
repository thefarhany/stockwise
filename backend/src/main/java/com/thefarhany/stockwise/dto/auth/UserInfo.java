package com.thefarhany.stockwise.dto.auth;

import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.auth.Language;
import java.util.UUID;

public record UserInfo(UUID id, String name, String email, String role, boolean active, Language language) {

	public static UserInfo from(AppUser user) {
		return new UserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.isActive(),
				user.getLanguage());
	}

}
