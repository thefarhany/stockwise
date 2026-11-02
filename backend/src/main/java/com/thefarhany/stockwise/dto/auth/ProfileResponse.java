package com.thefarhany.stockwise.dto.auth;

import com.thefarhany.stockwise.model.auth.AppUser;

public record ProfileResponse(UserInfo user, OutletInfo outlet) {

	public static ProfileResponse from(AppUser user) {
		return new ProfileResponse(UserInfo.from(user), OutletInfo.from(user.getOutlet()));
	}

}
