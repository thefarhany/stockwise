package com.thefarhany.stockwise.dto.outlet;

import com.thefarhany.stockwise.model.outlet.OutletAddress;
import jakarta.validation.constraints.Size;
import java.util.stream.Stream;

/**
 * Alamat usaha di request dan respons (outlet dan hasil geocoding). Batas panjang sama dengan kolom tabel outlet.
 * village = kelurahan/desa, district = kecamatan, city = kota/kabupaten.
 */
public record AddressDto(
		@Size(max = 255) String full,
		@Size(max = 150) String street,
		@Size(max = 100) String village,
		@Size(max = 100) String district,
		@Size(max = 100) String city,
		@Size(max = 100) String province,
		@Size(max = 10) String postalCode) {

	public static AddressDto of(OutletAddress a) {
		return a == null ? null
				: new AddressDto(a.full(), a.street(), a.village(), a.district(), a.city(), a.province(), a.postalCode());
	}

	/** Teks kosong jadi null; alamat yang semuanya kosong jadi null (tidak ada alamat). */
	public OutletAddress toModel() {
		OutletAddress a = new OutletAddress(clean(full), clean(street), clean(village), clean(district), clean(city),
				clean(province), clean(postalCode));
		boolean empty = Stream.of(a.full(), a.street(), a.village(), a.district(), a.city(), a.province(), a.postalCode())
				.allMatch(v -> v == null);
		return empty ? null : a;
	}

	private static String clean(String v) {
		return v == null || v.isBlank() ? null : v.trim();
	}

}
