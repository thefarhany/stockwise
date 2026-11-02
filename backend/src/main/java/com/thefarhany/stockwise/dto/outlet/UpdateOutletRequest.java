package com.thefarhany.stockwise.dto.outlet;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Semua field wajib kecuali latitude/longitude dan targetMarginPct. closeTime harus setelah openTime (dicek di
 * service). targetMarginPct kosong = tidak diubah (default 40%).
 */
public record UpdateOutletRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull LocalTime openTime,
		@NotNull LocalTime closeTime,
		@DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
		@NotNull @Size(max = 31) List<@NotNull @Min(1) @Max(31) Integer> paydayDays,
		@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal recommendationBufferPct,
		@DecimalMin("0") @DecimalMax("90") BigDecimal targetMarginPct) {
}
