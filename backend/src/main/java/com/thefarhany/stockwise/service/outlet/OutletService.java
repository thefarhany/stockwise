package com.thefarhany.stockwise.service.outlet;

import com.thefarhany.stockwise.dto.outlet.OutletResponse;
import com.thefarhany.stockwise.dto.outlet.SetLocationRequest;
import com.thefarhany.stockwise.dto.outlet.UpdateOutletRequest;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.model.outlet.OutletAddress;
import com.thefarhany.stockwise.repository.outlet.OutletDataRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.service.geo.MapboxGeocoder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OutletService {

	private final OutletRepository outlets;
	private final OutletDataRepository data;
	private final MapboxGeocoder geocoder;
	private final TransactionTemplate tx;

	public OutletService(OutletRepository outlets, OutletDataRepository data, MapboxGeocoder geocoder,
			TransactionTemplate tx) {
		this.outlets = outlets;
		this.data = data;
		this.geocoder = geocoder;
		this.tx = tx;
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
		if (r.currency() != null && r.currency() != outlet.getCurrency()) {
			if (data.onboardingCounts(outletId).hasSessions()) {
				throw new BusinessException(HttpStatus.CONFLICT, "CURRENCY_LOCKED",
						"Mata uang tidak bisa diganti setelah lapak pernah dibuka.");
			}
			outlet.setCurrency(r.currency());
		}
		outlet.setName(r.name().trim());
		outlet.setOpenTime(r.openTime());
		outlet.setCloseTime(r.closeTime());
		outlet.setLatitude(r.latitude());
		outlet.setLongitude(r.longitude());
		outlet.setAddress(r.address() == null ? null : r.address().toModel());
		outlet.setPaydayDays(r.paydayDays().stream().distinct().sorted().toList());
		outlet.setRecommendationBufferPct(r.recommendationBufferPct());
		if (r.targetMarginPct() != null) {
			outlet.setTargetMarginPct(r.targetMarginPct());
		}
		return response(outlet);
	}

	/**
	 * Simpan lokasi usaha. Tanpa alamat, alamat diambil dari Mapbox (reverse geocoding) lebih dulu, di luar transaksi
	 * supaya panggilan jaringan tidak menahan koneksi database; titik tanpa alamat dari Mapbox tetap disimpan.
	 * Koordinat yang disimpan selalu koordinat dari Owner, bukan titik hasil Mapbox.
	 */
	public OutletResponse setLocation(UUID outletId, SetLocationRequest r) {
		OutletAddress address = r.address() != null ? r.address().toModel()
				: geocoder.reverse(r.latitude(), r.longitude()).stream().findFirst()
						.map(p -> p.address().toModel()).orElse(null);
		return saveLocation(outletId, scale(r.latitude()), scale(r.longitude()), address);
	}

	public OutletResponse clearLocation(UUID outletId) {
		return saveLocation(outletId, null, null, null);
	}

	private OutletResponse saveLocation(UUID outletId, BigDecimal latitude, BigDecimal longitude, OutletAddress address) {
		return tx.execute(status -> {
			Outlet outlet = find(outletId);
			outlet.setLatitude(latitude);
			outlet.setLongitude(longitude);
			outlet.setAddress(address);
			return response(outlet);
		});
	}

	// kolom DECIMAL(9,6)
	private static BigDecimal scale(BigDecimal v) {
		return v.setScale(6, RoundingMode.HALF_UP);
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
