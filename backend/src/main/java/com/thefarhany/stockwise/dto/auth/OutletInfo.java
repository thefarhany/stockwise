package com.thefarhany.stockwise.dto.auth;

import com.thefarhany.stockwise.model.outlet.Currency;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.time.LocalTime;
import java.util.UUID;

public record OutletInfo(UUID id, String name, String timezone, Currency currency, LocalTime openTime, LocalTime closeTime,
		boolean setupCompleted) {

	public static OutletInfo from(Outlet o) {
		return new OutletInfo(o.getId(), o.getName(), o.getTimezone(), o.getCurrency(), o.getOpenTime(), o.getCloseTime(),
				OutletRules.isSetupCompleted(o));
	}

}
