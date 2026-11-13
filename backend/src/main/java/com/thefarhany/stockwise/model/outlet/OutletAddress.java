package com.thefarhany.stockwise.model.outlet;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Alamat usaha. full = alamat lengkap satu baris; sisanya bagian-bagiannya (village = kelurahan/desa,
 * district = kecamatan, city = kota/kabupaten). Semua boleh kosong; Hibernate menyimpan null untuk semua kolom bila
 * alamat tidak diisi.
 */
@Embeddable
public record OutletAddress(
		@Column(name = "address") String full,
		String street,
		String village,
		String district,
		String city,
		String province,
		@Column(name = "postal_code") String postalCode) {
}
