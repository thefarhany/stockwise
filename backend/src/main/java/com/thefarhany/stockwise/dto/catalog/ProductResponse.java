package com.thefarhany.stockwise.dto.catalog;

import com.thefarhany.stockwise.model.catalog.Product;
import com.thefarhany.stockwise.model.catalog.RecipeItem;
import com.thefarhany.stockwise.model.catalog.StockMode;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.utils.catalog.PriceSuggester;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * hpp, hppEstimated, margin, suggestedPrice hanya untuk Owner (null untuk Cashier) dan null bila produk belum beresep.
 * suggestedPrice = saran harga jual dari HPP dan target margin usaha (FR-53); ikut berstatus perkiraan bila hppEstimated.
 * hppEstimated true bila ada bahan yang belum pernah dibeli sehingga memakai harga perkiraan.
 * available / maxPortionsFromStock null bila tidak ada sesi terbuka atau produk belum beresep.
 */
public record ProductResponse(UUID id, String name, String category, long price, StockMode stockMode, Long hpp,
		Boolean hppEstimated, Long margin, Long suggestedPrice, Boolean available, Integer maxPortionsFromStock, boolean active,
		List<RecipeItemResponse> recipe) {

	/**
	 * @param averageCost harga rata-rata terbaru per bahan (per satuan bahan); bahan yang belum ada = pakai harga perkiraan
	 * @param available stok tersedia per bahan di sesi terbuka; null bila tidak ada sesi terbuka
	 * @param pricing usaha (target margin dan mata uang) untuk saran harga jual; null = tanpa saran harga
	 */
	public static ProductResponse from(Product p, List<RecipeItem> recipe, boolean showCost,
			Map<UUID, BigDecimal> averageCost, Map<UUID, BigDecimal> available, Outlet pricing) {
		Long hpp = null;
		Long margin = null;
		Boolean estimated = null;
		if (showCost && !recipe.isEmpty()) {
			BigDecimal sum = BigDecimal.ZERO;
			boolean usedEstimate = false;
			for (RecipeItem r : recipe) {
				BigDecimal unitCost = averageCost.get(r.getStockItem().getId());
				if (unitCost == null) {
					unitCost = BigDecimal.valueOf(r.getStockItem().getUnitCost());
					usedEstimate = true;
				}
				sum = sum.add(r.getQuantity().multiply(unitCost));
			}
			hpp = sum.setScale(0, RoundingMode.HALF_UP).longValueExact();
			margin = p.getPrice() - hpp;
			estimated = usedEstimate;
		}
		Boolean isAvailable = null;
		Integer maxPortions = null;
		if (available != null && !recipe.isEmpty()) {
			BigDecimal max = recipe.stream()
					.map(r -> available.getOrDefault(r.getStockItem().getId(), BigDecimal.ZERO)
							.max(BigDecimal.ZERO).divide(r.getQuantity(), 0, RoundingMode.DOWN))
					.min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
			maxPortions = max.min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue();
			isAvailable = maxPortions > 0;
		}
		return new ProductResponse(p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStockMode(), hpp,
				estimated, margin, pricing == null ? null : PriceSuggester.suggest(hpp, pricing.getTargetMarginPct(), pricing.getCurrency()), isAvailable, maxPortions, p.isActive(),
				recipe.stream().map(RecipeItemResponse::from).toList());
	}

}
