package com.thefarhany.stockwise.controller.outlet;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.outlet.DeleteOutletRequest;
import com.thefarhany.stockwise.dto.outlet.OutletResponse;
import com.thefarhany.stockwise.dto.outlet.SetLocationRequest;
import com.thefarhany.stockwise.dto.outlet.UpdateOutletRequest;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.outlet.OutletService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outlet")
@PreAuthorize("hasRole('OWNER')")
class OutletController {

	private final OutletService outletService;

	OutletController(OutletService outletService) {
		this.outletService = outletService;
	}

	@GetMapping
	ApiResponse<OutletResponse> get(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(outletService.get(me.outletId()));
	}

	@PutMapping
	ApiResponse<OutletResponse> update(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody UpdateOutletRequest request) {
		return ApiResponse.ok(outletService.update(me.outletId(), request));
	}

	/** Lokasi usaha: koordinat saja = alamat dicari lewat Mapbox; koordinat + alamat = disimpan apa adanya. */
	@PutMapping("/location")
	ApiResponse<OutletResponse> setLocation(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody SetLocationRequest request) {
		return ApiResponse.ok(outletService.setLocation(me.outletId(), request));
	}

	@DeleteMapping("/location")
	ApiResponse<OutletResponse> clearLocation(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(outletService.clearLocation(me.outletId()));
	}

	@DeleteMapping
	ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody DeleteOutletRequest request) {
		outletService.delete(me.outletId(), request.confirmName());
		return ApiResponse.ok(null);
	}

}
