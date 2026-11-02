package com.thefarhany.stockwise.service.catalog;

import com.thefarhany.stockwise.dto.catalog.ProductRequest;
import com.thefarhany.stockwise.dto.catalog.ProductResponse;
import com.thefarhany.stockwise.dto.catalog.RecipeItemRequest;
import com.thefarhany.stockwise.dto.catalog.RecipeItemResponse;
import com.thefarhany.stockwise.dto.catalog.RecipeRequest;
import com.thefarhany.stockwise.dto.catalog.RecipeResponse;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.auth.Role;
import com.thefarhany.stockwise.model.catalog.Product;
import com.thefarhany.stockwise.model.catalog.RecipeItem;
import com.thefarhany.stockwise.model.catalog.RecipeItemId;
import com.thefarhany.stockwise.model.catalog.StockItem;
import com.thefarhany.stockwise.model.catalog.StockMode;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.repository.catalog.ProductRepository;
import com.thefarhany.stockwise.repository.catalog.RecipeItemRepository;
import com.thefarhany.stockwise.repository.catalog.StockItemRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.session.SessionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Semua query dibatasi outletId dari token. */
@Service
public class ProductService {

	private final ProductRepository products;
	private final RecipeItemRepository recipes;
	private final StockItemRepository stockItems;
	private final OutletRepository outlets;
	private final SessionService sessionService;

	public ProductService(ProductRepository products, RecipeItemRepository recipes, StockItemRepository stockItems,
			OutletRepository outlets, SessionService sessionService) {
		this.sessionService = sessionService;
		this.products = products;
		this.recipes = recipes;
		this.stockItems = stockItems;
		this.outlets = outlets;
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> list(AuthPrincipal me, boolean includeInactive) {
		List<Product> found = includeInactive ? products.findByOutletIdOrderBySortOrderAscNameAsc(me.outletId())
				: products.findByOutletIdAndActiveTrueOrderBySortOrderAscNameAsc(me.outletId());
		Map<UUID, List<RecipeItem>> byProduct = recipes.findByProductOutletId(me.outletId()).stream()
				.collect(Collectors.groupingBy(r -> r.getProduct().getId()));
		boolean showCost = me.role() == Role.OWNER;
		Map<UUID, BigDecimal> cost = averageCost(me.outletId(), showCost);
		Map<UUID, BigDecimal> available = sessionService.availableStock(me.outletId()).orElse(null);
		BigDecimal margin = targetMargin(me.outletId(), showCost);
		return found.stream().map(p -> ProductResponse.from(p, byProduct.getOrDefault(p.getId(), List.of()), showCost,
				cost, available, margin)).toList();
	}

	@Transactional
	public ProductResponse create(UUID outletId, ProductRequest r) {
		String name = r.name().trim();
		if (products.existsByOutletIdAndNameIgnoreCase(outletId, name)) {
			throw duplicate();
		}
		Product product = new Product(outlets.getReferenceById(outletId));
		apply(product, name, r);
		return ProductResponse.from(products.save(product), List.of(), true, Map.of(), null, null);
	}

	@Transactional
	public ProductResponse update(UUID outletId, UUID id, ProductRequest r) {
		Product product = find(outletId, id);
		String name = r.name().trim();
		if (products.existsByOutletIdAndNameIgnoreCaseAndIdNot(outletId, name, id)) {
			throw duplicate();
		}
		apply(product, name, r);
		if (r.active() != null) {
			product.setActive(r.active());
		}
		return ProductResponse.from(product, recipes.findByProductId(id), true, averageCost(outletId, true),
				sessionService.availableStock(outletId).orElse(null), targetMargin(outletId, true));
	}

	/** DELETE = nonaktifkan; riwayat penjualan tetap utuh. */
	@Transactional
	public void deactivate(UUID outletId, UUID id) {
		find(outletId, id).setActive(false);
	}

	/** Mengganti seluruh resep. */
	@Transactional
	public RecipeResponse replaceRecipe(UUID outletId, UUID productId, RecipeRequest r) {
		Product product = find(outletId, productId);
		List<UUID> ids = r.items().stream().map(RecipeItemRequest::stockItemId).toList();
		if (ids.stream().distinct().count() != ids.size()) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
					Map.of("fields", Map.of("items", "bahan tidak boleh dobel")));
		}
		Map<UUID, StockItem> found = stockItems.findByIdInAndOutletIdAndActiveTrue(ids, outletId).stream()
				.collect(Collectors.toMap(StockItem::getId, Function.identity()));
		if (found.size() != ids.size()) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "STOCK_ITEM_NOT_FOUND", "Bahan tidak ditemukan.");
		}
		recipes.deleteByProductId(productId);
		List<RecipeItem> saved = recipes.saveAll(r.items().stream()
				.map(i -> new RecipeItem(new RecipeItemId(productId, i.stockItemId()), product, found.get(i.stockItemId()), i.quantity())).toList());
		return new RecipeResponse(productId, saved.stream().map(RecipeItemResponse::from).toList());
	}

	/** Target margin usaha untuk saran harga jual; hanya Owner yang melihat angka biaya. */
	private BigDecimal targetMargin(UUID outletId, boolean showCost) {
		return showCost ? outlets.findById(outletId).map(Outlet::getTargetMarginPct).orElse(null) : null;
	}

	/** Harga rata-rata hanya dibutuhkan (dan boleh dilihat) Owner. */
	private Map<UUID, BigDecimal> averageCost(UUID outletId, boolean showCost) {
		return showCost ? sessionService.latestAverageCost(outletId) : Map.of();
	}

	private Product find(UUID outletId, UUID id) {
		return products.findByIdAndOutletId(id, outletId).orElseThrow(
				() -> new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Produk tidak ditemukan."));
	}

	private static void apply(Product product, String name, ProductRequest r) {
		if (r.stockMode() == StockMode.BATCH) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
					Map.of("fields", Map.of("stockMode", "BATCH belum tersedia")));
		}
		String category = r.category() == null || r.category().isBlank() ? null : r.category().trim();
		product.setName(name);
		product.setCategory(category);
		product.setPrice(r.price());
		product.setStockMode(r.stockMode());
		product.setSortOrder(r.sortOrder().shortValue());
	}

	private static BusinessException duplicate() {
		return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Nama produk sudah dipakai.");
	}

}
