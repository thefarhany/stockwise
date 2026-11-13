package com.thefarhany.stockwise.controller.geo;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.geo.GeoPlaceResponse;
import com.thefarhany.stockwise.service.geo.MapboxGeocoder;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Alamat usaha dari koordinat atau teks, untuk profil usaha (Owner). */
@RestController
@RequestMapping("/api/v1/geo")
@PreAuthorize("hasRole('OWNER')")
class GeoController {

	private final MapboxGeocoder geocoder;

	GeoController(MapboxGeocoder geocoder) {
		this.geocoder = geocoder;
	}

	@GetMapping("/reverse")
	ApiResponse<List<GeoPlaceResponse>> reverse(
			@RequestParam @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
			@RequestParam @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
		return ApiResponse.ok(geocoder.reverse(latitude, longitude));
	}

	@GetMapping("/search")
	ApiResponse<List<GeoPlaceResponse>> search(@RequestParam @Size(min = 3, max = 200) String q,
			@RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
			@RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
		return ApiResponse.ok(geocoder.search(q.trim(), latitude, longitude));
	}

}
