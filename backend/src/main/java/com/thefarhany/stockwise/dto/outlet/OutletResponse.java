package com.thefarhany.stockwise.dto.outlet;

import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record OutletResponse(UUID id, String name, String timezone, LocalTime openTime, LocalTime closeTime,
		BigDecimal latitude, BigDecimal longitude, List<Integer> paydayDays, BigDecimal recommendationBufferPct,
		BigDecimal targetMarginPct, boolean setupCompleted, OnboardingInfo onboarding) {

	public static OutletResponse of(Outlet o, OnboardingCounts counts) {
		return new OutletResponse(o.getId(), o.getName(), o.getTimezone(), o.getOpenTime(), o.getCloseTime(),
				o.getLatitude(), o.getLongitude(), o.getPaydayDays(), o.getRecommendationBufferPct(), o.getTargetMarginPct(),
				OutletRules.isSetupCompleted(o), OnboardingInfo.of(OutletRules.isSetupCompleted(o), counts));
	}

}
