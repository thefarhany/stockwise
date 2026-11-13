package com.thefarhany.stockwise.dto.outlet;

import com.thefarhany.stockwise.model.outlet.Currency;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** currencyLocked = mata uang tidak bisa diganti lagi karena lapak sudah pernah dibuka. */
public record OutletResponse(UUID id, String name, String timezone, Currency currency, boolean currencyLocked,
		LocalTime openTime, LocalTime closeTime,
		BigDecimal latitude, BigDecimal longitude, AddressDto address, List<Integer> paydayDays, BigDecimal recommendationBufferPct,
		BigDecimal targetMarginPct, boolean setupCompleted, OnboardingInfo onboarding) {

	public static OutletResponse of(Outlet o, OnboardingCounts counts) {
		return new OutletResponse(o.getId(), o.getName(), o.getTimezone(), o.getCurrency(), counts.hasSessions(),
				o.getOpenTime(), o.getCloseTime(),
				o.getLatitude(), o.getLongitude(), AddressDto.of(o.getAddress()), o.getPaydayDays(), o.getRecommendationBufferPct(), o.getTargetMarginPct(),
				OutletRules.isSetupCompleted(o), OnboardingInfo.of(OutletRules.isSetupCompleted(o), counts));
	}

}
