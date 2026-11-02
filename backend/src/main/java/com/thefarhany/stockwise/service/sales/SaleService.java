package com.thefarhany.stockwise.service.sales;

import com.thefarhany.stockwise.dto.sales.CreateSaleRequest;
import com.thefarhany.stockwise.dto.sales.RecapRequest;
import com.thefarhany.stockwise.dto.sales.SaleDetailResponse;
import com.thefarhany.stockwise.dto.sales.SaleItemRequest;
import com.thefarhany.stockwise.dto.sales.SaleResponse;
import com.thefarhany.stockwise.dto.sales.SaleSummaryResponse;
import com.thefarhany.stockwise.dto.session.StockLevelResponse;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.auth.Role;
import com.thefarhany.stockwise.model.catalog.Product;
import com.thefarhany.stockwise.model.catalog.RecipeItem;
import com.thefarhany.stockwise.model.sales.InputMethod;
import com.thefarhany.stockwise.model.sales.PaymentMethod;
import com.thefarhany.stockwise.model.sales.Sale;
import com.thefarhany.stockwise.model.sales.SaleItem;
import com.thefarhany.stockwise.model.sales.SaleStatus;
import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.SessionStatus;
import com.thefarhany.stockwise.model.session.StockNeed;
import com.thefarhany.stockwise.repository.auth.AppUserRepository;
import com.thefarhany.stockwise.repository.catalog.ProductRepository;
import com.thefarhany.stockwise.repository.catalog.RecipeItemRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.repository.sales.SaleItemRepository;
import com.thefarhany.stockwise.repository.sales.SaleRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.session.SessionService;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Penjualan, rekap, void, dan riwayat. Perubahan stok selalu lewat SessionService, yang memegang kunci sesi.
 * Semua query dibatasi outletId dari token.
 */
@Service
public class SaleService {

	private final SaleRepository sales;
	private final SaleItemRepository saleItems;
	private final ProductRepository products;
	private final RecipeItemRepository recipes;
	private final OutletRepository outlets;
	private final AppUserRepository users;
	private final SessionService sessionService;
	private final Clock clock;

	public SaleService(SaleRepository sales, SaleItemRepository saleItems, ProductRepository products,
			RecipeItemRepository recipes, OutletRepository outlets, AppUserRepository users,
			SessionService sessionService, Clock clock) {
		this.sales = sales;
		this.saleItems = saleItems;
		this.products = products;
		this.recipes = recipes;
		this.outlets = outlets;
		this.users = users;
		this.sessionService = sessionService;
		this.clock = clock;
	}

	@Transactional
	public SaleResponse create(AuthPrincipal me, CreateSaleRequest r) {
		return record(me, r.sessionId(), r.paymentMethod(), r.cashReceived(), r.items(), InputMethod.TAP);
	}

	@Transactional
	public SaleResponse recap(AuthPrincipal me, RecapRequest r) {
		return record(me, r.sessionId(), r.paymentMethod(), null, r.items(), InputMethod.RECAP);
	}

	@Transactional
	public SaleDetailResponse voidSale(AuthPrincipal me, UUID id, String reason) {
		UUID sessionId = sales.findSessionId(id, me.outletId()).orElseThrow(SaleService::notFound);
		DailySession session = sessionService.lockOpen(me, sessionId);
		// dimuat setelah kunci didapat, supaya void ganda bersamaan melihat status terbaru
		Sale sale = sales.findByIdAndOutletId(id, me.outletId()).orElseThrow(SaleService::notFound);
		if (sale.getStatus() == SaleStatus.VOID) {
			throw new BusinessException(HttpStatus.CONFLICT, "SALE_ALREADY_VOID", "Transaksi ini sudah dibatalkan.");
		}
		Instant now = Instant.now(clock);
		AppUser user = users.getReferenceById(me.userId());
		sessionService.restoreStock(session, sale.getId(), user, now);
		sale.setStatus(SaleStatus.VOID);
		sale.setVoidReason(reason.trim());
		sale.setVoidedAt(now);
		sale.setVoidedBy(user);
		return SaleDetailResponse.from(sale, saleItems.findBySaleIdOrderByProductName(sale.getId()));
	}

	@Transactional(readOnly = true)
	public SaleDetailResponse get(AuthPrincipal me, UUID id) {
		Sale sale = sales.findByIdAndOutletId(id, me.outletId()).orElseThrow(SaleService::notFound);
		if (me.role() != Role.OWNER && sale.getSession().getStatus() != SessionStatus.OPEN) {
			throw forbidden();
		}
		return SaleDetailResponse.from(sale, saleItems.findBySaleIdOrderByProductName(sale.getId()));
	}

	/** Owner: sesi atau tanggal mana pun (tanpa filter = hari ini). Cashier: hanya sesi yang sedang berjalan. */
	@Transactional(readOnly = true)
	public Page<SaleSummaryResponse> history(AuthPrincipal me, UUID sessionId, LocalDate date, int page, int size) {
		PageRequest paging = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
				Sort.by(Sort.Direction.DESC, "receiptNo"));
		Optional<DailySession> session = me.role() == Role.OWNER ? ownerSession(me, sessionId, date)
				: cashierSession(me, sessionId, date);
		if (session.isEmpty()) {
			return new PageImpl<>(List.of(), paging, 0);
		}
		Page<Sale> found = sales.findBySessionId(session.get().getId(), paging);
		Map<UUID, Integer> totals = found.isEmpty() ? Map.of()
				: saleItems.totals(found.getContent().stream().map(Sale::getId).toList()).stream()
						.collect(Collectors.toMap(t -> t.getSaleId(), t -> t.getTotal().intValue()));
		return found.map(s -> SaleSummaryResponse.from(s, totals.getOrDefault(s.getId(), 0)));
	}

	private Optional<DailySession> ownerSession(AuthPrincipal me, UUID sessionId, LocalDate date) {
		if (sessionId != null) {
			return Optional.of(sessionService.find(me.outletId(), sessionId));
		}
		LocalDate day = date != null ? date : OutletRules.today(outlets.findById(me.outletId()).orElseThrow(), clock);
		return sessionService.findByDate(me.outletId(), day);
	}

	private Optional<DailySession> cashierSession(AuthPrincipal me, UUID sessionId, LocalDate date) {
		Optional<DailySession> open = sessionService.findOpen(me.outletId());
		boolean sameSession = open.isPresent() && (sessionId == null || open.get().getId().equals(sessionId));
		boolean sameDate = open.isPresent() && (date == null || open.get().getBusinessDate().equals(date));
		if ((sessionId != null || date != null) && !(sameSession && sameDate)) {
			throw forbidden();
		}
		return open;
	}

	private SaleResponse record(AuthPrincipal me, UUID sessionId, PaymentMethod method, Long cashReceived,
			List<SaleItemRequest> lines, InputMethod input) {
		DailySession session = sessionService.lockOpen(me, sessionId);

		Set<UUID> seen = new HashSet<>();
		for (SaleItemRequest line : lines) {
			if (!seen.add(line.productId())) {
				throw validation("items", "produk tidak boleh dobel");
			}
		}
		Map<UUID, Product> found = products.findByIdInAndOutletIdAndActiveTrue(seen, me.outletId()).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));
		if (found.size() != seen.size()) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Produk tidak ditemukan.");
		}
		Map<UUID, List<RecipeItem>> recipeByProduct = recipes.findByProductIdIn(seen).stream()
				.collect(Collectors.groupingBy(ri -> ri.getProduct().getId()));

		long total = 0;
		Map<UUID, StockNeed> needs = new LinkedHashMap<>();
		for (SaleItemRequest line : lines) {
			Product product = found.get(line.productId());
			List<RecipeItem> recipe = recipeByProduct.getOrDefault(product.getId(), List.of());
			if (recipe.isEmpty()) {
				throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "RECIPE_REQUIRED",
						"Produk " + product.getName() + " belum punya resep, jadi stoknya tidak bisa dikurangi.",
						Map.of("productId", product.getId()));
			}
			total += product.getPrice() * line.quantity();
			for (RecipeItem ri : recipe) {
				BigDecimal qty = ri.getQuantity().multiply(BigDecimal.valueOf(line.quantity()));
				needs.merge(ri.getStockItem().getId(), new StockNeed(ri.getStockItem(), qty),
						(a, b) -> new StockNeed(a.item(), a.quantity().add(b.quantity())));
			}
		}
		if (cashReceived != null) {
			if (method != PaymentMethod.CASH) {
				throw validation("cashReceived", "hanya untuk pembayaran tunai");
			}
			if (cashReceived < total) {
				throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CASH_INSUFFICIENT",
						"Uang diterima kurang dari total.", Map.of("totalAmount", total, "cashReceived", cashReceived));
			}
		}

		Instant now = Instant.now(clock);
		AppUser user = users.getReferenceById(me.userId());
		// kunci sesi sudah dipegang, jadi nomor struk berikutnya tidak bisa direbut transaksi lain
		Sale sale = sales.save(new Sale(session.getOutlet(), session, sales.maxReceiptNo(session.getId()) + 1, now, total, method,
				cashReceived, input, user));
		List<SaleItem> items = new ArrayList<>();
		for (SaleItemRequest line : lines) {
			Product product = found.get(line.productId());
			items.add(new SaleItem(sale, product, BigDecimal.valueOf(line.quantity()), product.getPrice(),
					product.getPrice() * line.quantity()));
		}
		saleItems.saveAll(items);
		List<StockLevelResponse> stockAfter = sessionService.consumeStock(session, needs.values(), sale.getId(), user,
				now);
		return SaleResponse.from(sale, items, stockAfter);
	}

	private static BusinessException notFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "SALE_NOT_FOUND", "Transaksi tidak ditemukan.");
	}

	private static BusinessException forbidden() {
		return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Tidak punya akses.");
	}

	private static BusinessException validation(String field, String message) {
		return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
				Map.of("fields", Map.of(field, message)));
	}

}
