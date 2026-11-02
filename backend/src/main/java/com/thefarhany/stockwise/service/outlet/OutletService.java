package com.thefarhany.stockwise.service.outlet;

import com.thefarhany.stockwise.dto.outlet.OutletResponse;
import com.thefarhany.stockwise.dto.outlet.UpdateOutletRequest;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.repository.outlet.OutletDataRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutletService {

	private final OutletRepository outlets;
	private final OutletDataRepository data;

	public OutletService(OutletRepository outlets, OutletDataRepository data) {
		this.outlets = outlets;
		this.data = data;
	}

	@Transactional(readOnly = true)
	public OutletResponse get(UUID outletId) {
		return response(find(outletId));
	}

	@Transactional
	public OutletResponse update(UUID outletId, UpdateOutletRequest r) {
		if (!r.closeTime().isAfter(r.openTime())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
					Map.of("fields", Map.of("closeTime", "harus sesudah jam buka")));
		}
		Outlet outlet = find(outletId);
		outlet.setName(r.name().trim());
		outlet.setOpenTime(r.openTime());
		outlet.setCloseTime(r.closeTime());
		outlet.setLatitude(r.latitude());
		outlet.setLongitude(r.longitude());
		outlet.setPaydayDays(r.paydayDays().stream().distinct().sorted().toList());
		outlet.setRecommendationBufferPct(r.recommendationBufferPct());
		if (r.targetMarginPct() != null) {
			outlet.setTargetMarginPct(r.targetMarginPct());
		}
		return response(outlet);
	}

	/** Permanen: usaha, staf, dan seluruh riwayat (FR-49). */
	@Transactional
	public void delete(UUID outletId, String confirmName) {
		Outlet outlet = find(outletId);
		if (!outlet.getName().equals(confirmName)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
					Map.of("fields", Map.of("confirmName", "harus sama persis dengan nama usaha")));
		}
		data.purge(outletId);
	}

	private Outlet find(UUID outletId) {
		return outlets.findById(outletId)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Perlu login."));
	}

	private OutletResponse response(Outlet outlet) {
		return OutletResponse.of(outlet, data.onboardingCounts(outlet.getId()));
	}

}
