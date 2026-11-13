package com.thefarhany.stockwise.dto.geo;

import com.thefarhany.stockwise.dto.outlet.AddressDto;
import java.math.BigDecimal;

/** Satu hasil geocoding: alamat (bentuk sama dengan alamat outlet) dan titik koordinatnya. */
public record GeoPlaceResponse(AddressDto address, BigDecimal latitude, BigDecimal longitude) {
}
