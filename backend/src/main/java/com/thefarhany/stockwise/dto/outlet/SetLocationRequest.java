package com.thefarhany.stockwise.dto.outlet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Lokasi usaha. address kosong = alamat dicari dari koordinat lewat Mapbox ("Use my current location"); address
 * diisi = alamat itu yang disimpan (hasil pencarian yang dipilih Owner).
 */
public record SetLocationRequest(
		@NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
		@Valid AddressDto address) {
}
