package com.thefarhany.stockwise.service.session;

import com.thefarhany.stockwise.dto.outlet.OnboardingInfo;
import com.thefarhany.stockwise.dto.session.AddStockRequest;
import com.thefarhany.stockwise.dto.session.CloseItemRequest;
import com.thefarhany.stockwise.dto.session.CloseSessionRequest;
import com.thefarhany.stockwise.dto.session.OpenSessionRequest;
import com.thefarhany.stockwise.dto.session.SessionResponse;
import com.thefarhany.stockwise.dto.session.StockLevelResponse;
import com.thefarhany.stockwise.dto.session.StockLineRequest;
import com.thefarhany.stockwise.exception.BusinessException;
import com.thefarhany.stockwise.model.auth.AppUser;
import com.thefarhany.stockwise.model.auth.Role;
import com.thefarhany.stockwise.model.catalog.StockItem;
import com.thefarhany.stockwise.model.catalog.StockKind;
import com.thefarhany.stockwise.model.outlet.Outlet;
import com.thefarhany.stockwise.model.session.CloseType;
import com.thefarhany.stockwise.model.session.DailySession;
import com.thefarhany.stockwise.model.session.MovementType;
import com.thefarhany.stockwise.model.session.SessionClosedEvent;
import com.thefarhany.stockwise.model.session.SessionOpenedEvent;
import com.thefarhany.stockwise.model.session.SessionStatus;
import com.thefarhany.stockwise.model.session.SessionStock;
import com.thefarhany.stockwise.model.session.StockMovement;
import com.thefarhany.stockwise.model.session.StockNeed;
import com.thefarhany.stockwise.repository.auth.AppUserRepository;
import com.thefarhany.stockwise.repository.catalog.StockItemRepository;
import com.thefarhany.stockwise.repository.outlet.OutletDataRepository;
import com.thefarhany.stockwise.repository.outlet.OutletRepository;
import com.thefarhany.stockwise.repository.session.DailySessionRepository;
import com.thefarhany.stockwise.repository.session.ItemCost;
import com.thefarhany.stockwise.repository.session.SessionStockRepository;
import com.thefarhany.stockwise.repository.session.StockMovementRepository;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.utils.Money;
import com.thefarhany.stockwise.utils.catalog.UnitConverter;
import com.thefarhany.stockwise.utils.outlet.OutletRules;
import com.thefarhany.stockwise.utils.session.SessionRules;
import com.thefarhany.stockwise.utils.session.StockCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Siklus sesi harian: buka, tambah stok, tutup (manual atau otomatis). Semua operasi yang mengubah stok mengunci
 * baris sesi lebih dulu, sehingga dua perangkat berjalan bergiliran. Sesi CLOSED final.
 */
@Service
public class SessionService {

	private static final Logger log = LoggerFactory.getLogger(SessionService.class);

	private record Count(BigDecimal closing, BigDecimal carryOver, BigDecimal discard) {
	}

	private final DailySessionRepository sessions;
	private final SessionStockRepository stocks;
	private final StockMovementRepository movements;
	private final StockItemRepository stockItems;
	private final OutletRepository outlets;
	private final OutletDataRepository outletData;
	private final AppUserRepository users;
	private final Clock clock;
	private final TransactionTemplate tx;
	private final ApplicationEventPublisher events;

	public SessionService(DailySessionRepository sessions, SessionStockRepository stocks,
			StockMovementRepository movements, StockItemRepository stockItems, OutletRepository outlets,
			OutletDataRepository outletData, AppUserRepository users, Clock clock,
			PlatformTransactionManager txManager, ApplicationEventPublisher events) {
		this.events = events;
		this.sessions = sessions;
		this.stocks = stocks;
		this.movements = movements;
		this.stockItems = stockItems;
		this.outlets = outlets;
		this.outletData = outletData;
		this.users = users;
		this.clock = clock;
		this.tx = new TransactionTemplate(txManager);
	}

	/** Sesi terbuka milik usaha ini, atau null bila tidak ada. */
	@Transactional(readOnly = true)
	public SessionResponse current(AuthPrincipal me) {
		return sessions.findByOutletIdAndStatus(me.outletId(), SessionStatus.OPEN).map(s -> respond(s, me)).orElse(null);
	}

	@Transactional(readOnly = true)
	public SessionResponse detail(AuthPrincipal me, UUID id) {
		return respond(sessions.findByIdAndOutletId(id, me.outletId()).orElseThrow(SessionService::notFound), me);
	}

	@Transactional
	public SessionResponse open(AuthPrincipal me, OpenSessionRequest request) {
		Outlet outlet = outlets.findById(me.outletId())
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Perlu login."));
		OnboardingInfo onboarding = OnboardingInfo.of(OutletRules.isSetupCompleted(outlet),
				outletData.onboardingCounts(outlet.getId()));
		if (!onboarding.readyToOpen()) {
			throw new BusinessException(HttpStatus.CONFLICT, "SETUP_REQUIRED",
					"Lengkapi profil, bahan, produk, dan resep sebelum membuka lapak.",
					Map.of("missing", onboarding.missing()));
		}
		sessions.findByOutletIdAndStatus(outlet.getId(), SessionStatus.OPEN).ifPresent(open -> {
			throw new BusinessException(HttpStatus.CONFLICT, "SESSION_ALREADY_OPEN",
					"Masih ada sesi yang terbuka untuk usaha ini.", Map.of("openSessionId", open.getId()));
		});
		Instant now = Instant.now(clock);
		LocalDate today = OutletRules.today(outlet, clock);
		if (sessions.existsByOutletIdAndBusinessDate(outlet.getId(), today)) {
			throw new BusinessException(HttpStatus.CONFLICT, "SESSION_DATE_EXISTS",
					"Sesi untuk tanggal ini sudah pernah dibuka dan tidak bisa dibuka lagi.");
		}
		AppUser user = users.getReferenceById(me.userId());
		DailySession session = new DailySession(outlet, today, now, user, outlet.getCloseTime());
		if (!now.isBefore(SessionRules.autoCloseAt(session))) {
			throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "SESSION_CUTOFF_PASSED",
					"Sudah lewat batas buka hari ini (jam tutup + 30 menit).");
		}
		List<StockLineRequest> lines = request == null || request.stock() == null ? List.of() : request.stock();
		validateLines(lines, "stock");
		sessions.save(session);

		Map<UUID, SessionStock> rows = openRows(session, user, now);
		applyLines(session, rows, lines, "stock", user, now);
		events.publishEvent(new SessionOpenedEvent(session));
		return respond(session, me);
	}

	@Transactional
	public SessionResponse addStock(AuthPrincipal me, UUID id, AddStockRequest request) {
		DailySession session = lockOpen(me, id);
		validateLines(request.items(), "items");
		Instant now = Instant.now(clock);
		Map<UUID, SessionStock> rows = stocks.findBySessionIdOrderByStockItemName(id).stream()
				.collect(Collectors.toMap(r -> r.getStockItem().getId(), Function.identity()));
		applyLines(session, rows, request.items(), "items", users.getReferenceById(me.userId()), now);
		return respond(session, me);
	}

	@Transactional
	public SessionResponse close(AuthPrincipal me, UUID id, CloseSessionRequest request) {
		DailySession session = lockOpen(me, id);
		List<SessionStock> rows = stocks.findBySessionIdOrderByStockItemName(id);
		Map<UUID, Count> counts = validateCounts(rows, request.items());
		AppUser user = users.getReferenceById(me.userId());
		finalizeClose(session, rows, counts, CloseType.MANUAL, user, request.notes(), Instant.now(clock));
		return respond(session, me);
	}

	/** Dipanggil penjadwal. Menutup sesi yang sudah melewati batas; tiap sesi punya transaksi sendiri. */
	public int autoCloseDue() {
		Instant now = Instant.now(clock);
		int closed = 0;
		for (DailySession due : sessions.findByStatus(SessionStatus.OPEN)) {
			if (now.isBefore(SessionRules.autoCloseAt(due))) {
				continue;
			}
			try {
				if (Boolean.TRUE.equals(tx.execute(status -> autoClose(due.getId(), now)))) {
					closed++;
				}
			}
			catch (RuntimeException ex) {
				log.error("auto_close_failed sessionId={}", due.getId(), ex);
			}
		}
		return closed;
	}

	/** Stok tersedia per bahan di sesi yang sedang terbuka; kosong (Optional) bila tidak ada sesi terbuka. */
	@Transactional(readOnly = true)
	public Optional<Map<UUID, BigDecimal>> availableStock(UUID outletId) {
		return sessions.findByOutletIdAndStatus(outletId, SessionStatus.OPEN)
				.map(s -> stocks.findBySessionIdOrderByStockItemName(s.getId()).stream()
						.collect(Collectors.toMap(r -> r.getStockItem().getId(), StockCalculator::available)));
	}

	/** Harga rata-rata terbaru per bahan (per satuan bahan), dari sesi terbaru yang memuat bahan itu. */
	@Transactional(readOnly = true)
	public Map<UUID, BigDecimal> latestAverageCost(UUID outletId) {
		return stocks.latestItemCosts(outletId).stream()
				.collect(Collectors.toMap(ItemCost::stockItemId, ItemCost::average));
	}

	/**
	 * Mengurangi stok untuk penjualan. Semua atau tidak sama sekali: bila ada bahan kurang, tidak ada yang berubah.
	 * Sesi harus sudah dikunci lewat lockOpen.
	 */
	public List<StockLevelResponse> consumeStock(DailySession session, Collection<StockNeed> needs, UUID saleId,
			AppUser user, Instant now) {
		Map<UUID, SessionStock> rows = stocks.findBySessionIdOrderByStockItemName(session.getId()).stream()
				.collect(Collectors.toMap(r -> r.getStockItem().getId(), Function.identity()));
		List<Map<String, Object>> shortages = new ArrayList<>();
		for (StockNeed need : needs) {
			SessionStock row = rows.get(need.item().getId());
			BigDecimal available = row == null ? BigDecimal.ZERO : StockCalculator.available(row);
			if (available.compareTo(need.quantity()) < 0) {
				shortages.add(Map.of("stockItemId", need.item().getId(), "name", need.item().getName(), "needed",
						need.quantity(), "available", available.max(BigDecimal.ZERO)));
			}
		}
		if (!shortages.isEmpty()) {
			throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_STOCK",
					"Stok bahan tidak cukup.", Map.of("shortages", shortages));
		}
		List<StockMovement> entries = new ArrayList<>();
		List<StockLevelResponse> after = new ArrayList<>();
		for (StockNeed need : needs) {
			SessionStock row = rows.get(need.item().getId());
			StockCalculator.sell(row, need.quantity(), now);
			entries.add(movement(session, need.item(), MovementType.SALE, need.quantity().negate(), saleId, user, now));
			after.add(StockLevelResponse.from(row));
		}
		movements.saveAll(entries);
		return after;
	}

	/** Mengembalikan stok persis seperti yang dikurangi transaksi itu (dari buku besar, bukan resep saat ini). */
	public void restoreStock(DailySession session, UUID saleId, AppUser user, Instant now) {
		List<StockMovement> sold = movements.findByReferenceIdAndType(saleId, MovementType.SALE);
		Map<UUID, SessionStock> rows = stocks.findBySessionIdOrderByStockItemName(session.getId()).stream()
				.collect(Collectors.toMap(r -> r.getStockItem().getId(), Function.identity()));
		List<StockMovement> entries = new ArrayList<>();
		for (StockMovement m : sold) {
			BigDecimal qty = m.getQuantity().negate();
			StockCalculator.restore(rows.get(m.getStockItem().getId()), qty);
			entries.add(movement(session, m.getStockItem(), MovementType.VOID, qty, saleId, user, now));
		}
		movements.saveAll(entries);
	}

	/** Sesi terbuka usaha ini, bila ada. */
	@Transactional(readOnly = true)
	public Optional<DailySession> findOpen(UUID outletId) {
		return sessions.findByOutletIdAndStatus(outletId, SessionStatus.OPEN);
	}

	@Transactional(readOnly = true)
	public DailySession find(UUID outletId, UUID id) {
		return sessions.findByIdAndOutletId(id, outletId).orElseThrow(SessionService::notFound);
	}

	@Transactional(readOnly = true)
	public Optional<DailySession> findByDate(UUID outletId, java.time.LocalDate date) {
		return sessions.findByOutletIdAndBusinessDate(outletId, date);
	}

	private boolean autoClose(UUID id, Instant now) {
		DailySession session = sessions.lockById(id).orElse(null);
		if (session == null || session.getStatus() != SessionStatus.OPEN
				|| now.isBefore(SessionRules.autoCloseAt(session))) {
			return false;
		}
		List<SessionStock> rows = stocks.findBySessionIdOrderByStockItemName(id);
		// Hitungan akhir = stok tercatat; yang boleh dibawa ikut terbawa, sisanya dianggap terbuang.
		Map<UUID, Count> counts = new LinkedHashMap<>();
		for (SessionStock row : rows) {
			BigDecimal recorded = StockCalculator.available(row).max(BigDecimal.ZERO);
			BigDecimal carry = row.getStockItem().isCarryOverAllowed() ? recorded : BigDecimal.ZERO;
			counts.put(row.getStockItem().getId(), new Count(recorded, carry, recorded.subtract(carry)));
		}
		finalizeClose(session, rows, counts, CloseType.AUTO, null, null, now);
		return true;
	}

	/** Sisa dari sesi tutup terakhir menjadi stok awal (jumlah, umur +1 hari, nilai). */
	private Map<UUID, SessionStock> openRows(DailySession session, AppUser user, Instant now) {
		Map<UUID, StockItem> items = new LinkedHashMap<>();
		stockItems.findByOutletIdAndActiveTrueOrderByName(session.getOutlet().getId())
				.forEach(i -> items.put(i.getId(), i));
		Map<UUID, SessionStock> previous = sessions
				.findFirstByOutletIdAndStatusOrderByBusinessDateDesc(session.getOutlet().getId(), SessionStatus.CLOSED)
				.map(p -> stocks.findBySessionIdOrderByStockItemName(p.getId()).stream()
						.collect(Collectors.toMap(r -> r.getStockItem().getId(), Function.identity())))
				.orElse(Map.of());
		// bahan yang sudah dinonaktifkan tetapi masih ada sisanya tidak boleh hilang diam-diam
		previous.values().stream().filter(r -> r.getCarriedOverQty() != null && r.getCarriedOverQty().signum() > 0)
				.forEach(r -> items.putIfAbsent(r.getStockItem().getId(), r.getStockItem()));

		Map<UUID, SessionStock> rows = new LinkedHashMap<>();
		List<StockMovement> opening = new ArrayList<>();
		for (StockItem item : items.values()) {
			SessionStock row = new SessionStock(session, item);
			SessionStock prev = previous.get(item.getId());
			if (prev != null && prev.getCarriedOverQty() != null && prev.getCarriedOverQty().signum() > 0) {
				row.setOpeningQty(prev.getCarriedOverQty());
				row.setOpeningAgeDays((short) (prev.getOpeningAgeDays() + 1));
				row.setOpeningValue(prev.getCarriedValue());
				opening.add(new StockMovement(session, item, MovementType.OPENING, prev.getCarriedOverQty(), null,
						null, user, now));
			}
			rows.put(item.getId(), stocks.save(row));
		}
		movements.saveAll(opening);
		return rows;
	}

	/** Validasi baris belanja tanpa menyentuh database: dobel dan aturan biaya. */
	private void validateLines(List<StockLineRequest> lines, String field) {
		Set<UUID> seen = new HashSet<>();
		for (StockLineRequest line : lines) {
			if (!seen.add(line.stockItemId())) {
				throw validation(field, "bahan tidak boleh dobel");
			}
			boolean hasCost = line.totalCost() != null && line.totalCost() > 0;
			boolean noCost = Boolean.TRUE.equals(line.noCost());
			if (hasCost == noCost) {
				throw new BusinessException(HttpStatus.BAD_REQUEST, "COST_REQUIRED",
						"Isi total bayar (lebih dari 0) atau tandai tanpa biaya, salah satu saja.",
						Map.of("stockItemId", line.stockItemId()));
			}
		}
	}

	private void applyLines(DailySession session, Map<UUID, SessionStock> rows, List<StockLineRequest> lines,
			String field, AppUser user, Instant now) {
		if (lines.isEmpty()) {
			return;
		}
		List<UUID> ids = lines.stream().map(StockLineRequest::stockItemId).toList();
		Map<UUID, StockItem> found = stockItems.findByIdInAndOutletIdAndActiveTrue(ids, session.getOutlet().getId())
				.stream().collect(Collectors.toMap(StockItem::getId, Function.identity()));
		if (found.size() != ids.size()) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "STOCK_ITEM_NOT_FOUND", "Bahan tidak ditemukan.");
		}
		List<StockMovement> added = new ArrayList<>();
		for (StockLineRequest line : lines) {
			StockItem item = found.get(line.stockItemId());
			BigDecimal qty = toItemUnit(line, item, field);
			long cost = Boolean.TRUE.equals(line.noCost()) ? 0L : line.totalCost();
			SessionStock row = rows.computeIfAbsent(item.getId(), k -> stocks.save(new SessionStock(session, item)));
			StockCalculator.addStock(row, qty, cost);
			MovementType type = item.getKind() == StockKind.RAW ? MovementType.PURCHASE : MovementType.PRODUCTION;
			BigDecimal unitCost = BigDecimal.valueOf(cost).divide(qty, 4, RoundingMode.HALF_UP);
			if (unitCost.compareTo(BigDecimal.valueOf(Money.MAX_UNIT_PRICE)) > 0) {
				throw validation(field, "harga per satuan terlalu besar; periksa jumlah dan total bayar");
			}
			added.add(new StockMovement(session, item, type, qty, unitCost, null, user, now));
		}
		movements.saveAll(added);
	}

	private static BigDecimal toItemUnit(StockLineRequest line, StockItem item, String field) {
		if (line.unit() == null || line.unit() == item.getUnit()) {
			return line.quantity();
		}
		if (!UnitConverter.convertible(line.unit(), item.getUnit())) {
			throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "UNIT_MISMATCH",
					"Satuan " + line.unit() + " tidak sekeluarga dengan satuan bahan (" + item.getUnit() + ").",
					Map.of("stockItemId", item.getId(), "itemUnit", item.getUnit()));
		}
		BigDecimal converted = UnitConverter.convert(line.quantity(), line.unit(), item.getUnit());
		if (converted.signum() <= 0) {
			throw validation(field, "jumlah terlalu kecil setelah dikonversi");
		}
		return converted;
	}

	private Map<UUID, Count> validateCounts(List<SessionStock> rows, List<CloseItemRequest> items) {
		Map<UUID, SessionStock> byItem = rows.stream()
				.collect(Collectors.toMap(r -> r.getStockItem().getId(), Function.identity()));
		Map<UUID, Count> counts = new LinkedHashMap<>();
		for (CloseItemRequest i : items) {
			SessionStock row = byItem.get(i.stockItemId());
			if (row == null) {
				throw new BusinessException(HttpStatus.NOT_FOUND, "STOCK_ITEM_NOT_FOUND", "Bahan tidak ditemukan.");
			}
			if (counts.put(i.stockItemId(), new Count(i.closingQty(), i.carryOverQty(), i.discardQty())) != null) {
				throw validation("items", "bahan tidak boleh dobel");
			}
			if (i.carryOverQty().add(i.discardQty()).compareTo(i.closingQty()) != 0) {
				throw validation("items", row.getStockItem().getName() + ": simpan + buang harus sama dengan sisa");
			}
			if (i.carryOverQty().signum() > 0 && !row.getStockItem().isCarryOverAllowed()) {
				throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CARRY_OVER_NOT_ALLOWED",
						row.getStockItem().getName() + " tidak boleh dibawa ke besok.",
						Map.of("stockItemId", i.stockItemId()));
			}
		}
		if (counts.size() != rows.size()) {
			throw validation("items", "semua bahan sesi harus dihitung");
		}
		return counts;
	}

	/** Langkah bersama tutup manual dan otomatis: selisih, bawa, buang, biaya, lalu status CLOSED. */
	private void finalizeClose(DailySession session, List<SessionStock> rows, Map<UUID, Count> counts, CloseType type,
			AppUser by, String notes, Instant now) {
		List<StockMovement> entries = new ArrayList<>();
		for (SessionStock row : rows) {
			Count c = counts.get(row.getStockItem().getId());
			BigDecimal diff = StockCalculator.applyClose(row, c.closing(), c.carryOver(), c.discard());
			StockItem item = row.getStockItem();
			if (diff.signum() != 0) {
				entries.add(new StockMovement(session, item, MovementType.ADJUSTMENT, diff, null,
						"Selisih hitungan akhir sesi", by, now));
			}
			if (c.carryOver().signum() > 0) {
				entries.add(new StockMovement(session, item, MovementType.CARRY_OVER, c.carryOver().negate(), null, null,
						by, now));
			}
			if (c.discard().signum() > 0) {
				entries.add(new StockMovement(session, item, MovementType.DISCARD, c.discard().negate(), null, null, by,
						now));
			}
		}
		movements.saveAll(entries);
		session.setStatus(SessionStatus.CLOSED);
		session.setClosedAt(now);
		session.setCloseType(type);
		session.setClosedBy(by);
		session.setNotes(notes);
		events.publishEvent(new SessionClosedEvent(session));
	}

	/** Kunci baris sesi lalu pastikan masih terbuka; semua operasi yang mengubah stok harus lewat sini. */
	public DailySession lockOpen(AuthPrincipal me, UUID id) {
		DailySession session = sessions.lockByIdAndOutletId(id, me.outletId()).orElseThrow(SessionService::notFound);
		if (session.getStatus() != SessionStatus.OPEN) {
			throw new BusinessException(HttpStatus.CONFLICT, "SESSION_NOT_OPEN",
					"Sesi sudah ditutup dan datanya tidak bisa diubah.");
		}
		return session;
	}

	private SessionResponse respond(DailySession session, AuthPrincipal me) {
		return SessionResponse.from(session, stocks.findBySessionIdOrderByStockItemName(session.getId()),
				me.role() == Role.OWNER);
	}

	/** Pergerakan stok yang mengacu ke sebuah transaksi (sale.id). */
	private static StockMovement movement(DailySession session, StockItem item, MovementType type, BigDecimal quantity,
			UUID referenceId, AppUser user, Instant at) {
		StockMovement movement = new StockMovement(session, item, type, quantity, null, null, user, at);
		movement.setReferenceId(referenceId);
		return movement;
	}

	private static BusinessException notFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Sesi tidak ditemukan.");
	}

	private static BusinessException validation(String field, String message) {
		return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validasi gagal.",
				Map.of("fields", Map.of(field, message)));
	}

}
