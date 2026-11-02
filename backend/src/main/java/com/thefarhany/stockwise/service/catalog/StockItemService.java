package com.thefarhany.stockwise.service.catalog;

import com.thefarhany.stockwise.dto.catalog.StockItemRequest;
import com.thefarhany.stockwise.dto.catalog.StockItemResponse;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.auth.Role;
import com.thefarhany.stockwise.model.catalog.StockItem;
import com.thefarhany.stockwise.repository.catalog.RecipeItemRepository;
import com.thefarhany.stockwise.repository.catalog.StockItemRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Semua query dibatasi outletId dari token. */
@Service
public class StockItemService {

	private final StockItemRepository items;
	private final RecipeItemRepository recipes;
	private final OutletRepository outlets;

	public StockItemService(StockItemRepository items, RecipeItemRepository recipes, OutletRepository outlets) {
		this.items = items;
		this.recipes = recipes;
		this.outlets = outlets;
	}

	@Transactional(readOnly = true)
	public List<StockItemResponse> list(AuthPrincipal me, boolean includeInactive) {
		List<StockItem> found = includeInactive ? items.findByOutletIdOrderByName(me.outletId())
				: items.findByOutletIdAndActiveTrueOrderByName(me.outletId());
		boolean showCost = me.role() == Role.OWNER;
		return found.stream().map(s -> StockItemResponse.from(s, showCost)).toList();
	}

	@Transactional
	public StockItemResponse create(UUID outletId, StockItemRequest r) {
		String name = r.name().trim();
		if (items.existsByOutletIdAndNameIgnoreCase(outletId, name)) {
			throw duplicate();
		}
		StockItem item = new StockItem(outlets.getReferenceById(outletId));
		apply(item, name, r);
		return StockItemResponse.from(items.save(item), true);
	}

	@Transactional
	public StockItemResponse update(UUID outletId, UUID id, StockItemRequest r) {
		StockItem item = find(outletId, id);
		String name = r.name().trim();
		if (items.existsByOutletIdAndNameIgnoreCaseAndIdNot(outletId, name, id)) {
			throw duplicate();
		}
		if (item.getUnit() != r.unit() && items.sessionUsageCount(id) > 0) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
					Map.of("fields", Map.of("unit", "tidak bisa diubah, bahan sudah dipakai di sesi")));
		}
		apply(item, name, r);
		if (r.active() != null) {
			setActive(item, r.active());
		}
		return StockItemResponse.from(item, true);
	}

	/** DELETE = nonaktifkan; riwayat tetap utuh. */
	@Transactional
	public void deactivate(UUID outletId, UUID id) {
		setActive(find(outletId, id), false);
	}

	private void setActive(StockItem item, boolean active) {
		if (!active && item.isActive()) {
			List<String> products = recipes.activeProductNamesUsing(item.getId());
			if (!products.isEmpty()) {
				throw new BusinessException(HttpStatus.CONFLICT, "STOCK_ITEM_IN_USE",
						"Bahan masih dipakai resep produk aktif.", Map.of("products", products));
			}
		}
		item.setActive(active);
	}

	private StockItem find(UUID outletId, UUID id) {
		return items.findByIdAndOutletId(id, outletId).orElseThrow(
				() -> new BusinessException(HttpStatus.NOT_FOUND, "STOCK_ITEM_NOT_FOUND", "Bahan tidak ditemukan."));
	}

	private static void apply(StockItem item, String name, StockItemRequest r) {
		item.setName(name);
		item.setKind(r.kind());
		item.setUnit(r.unit());
		item.setUnitCost(r.unitCost());
		item.setShelfLifeDays(r.shelfLifeDays().shortValue());
		item.setCarryOverAllowed(r.carryOverAllowed());
		item.setPrimary(r.isPrimary());
	}

	private static BusinessException duplicate() {
		return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Nama bahan sudah dipakai.");
	}

}
