package com.thefarhany.stockwise.utils.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.thefarhany.stockwise.model.catalog.StockItem;
import com.thefarhany.stockwise.model.session.SessionStock;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StockCalculatorTest {

	private static BigDecimal d(String v) {
		return new BigDecimal(v);
	}

	private static SessionStock row(boolean primary, String openingQty, long openingValue, String added, long addedCost,
			String sold) {
		StockItem item = new StockItem(null);
		item.setPrimary(primary);
		SessionStock s = new SessionStock(null, item);
		s.setOpeningQty(d(openingQty));
		s.setOpeningValue(openingValue);
		if (d(added).signum() > 0) {
			StockCalculator.addStock(s, d(added), addedCost);
		}
		s.setSoldQty(d(sold));
		return s;
	}

	private static SessionStock row(String openingQty, long openingValue, String added, long addedCost, String sold) {
		return row(false, openingQty, openingValue, added, addedCost, sold);
	}

	private static void assertBalanced(SessionStock s) {
		assertThat(s.getOpeningValue() + s.getAddedCost())
				.isEqualTo(s.getSoldCost() + s.getDiscardedCost() + s.getAdjustmentCost() + s.getCarriedValue());
	}

	@Test
	void availableIsOpeningPlusAddedPlusAdjustedMinusSold() {
		SessionStock s = row("8", 56000, "100", 700000, "30");
		s.setAdjustedQty(d("-2"));
		assertThat(StockCalculator.available(s)).isEqualByComparingTo("76");
	}

	@Test
	void weightedAverageSplitsValueAndBalancesExactly() {
		// 8 sisa @ 56.000 + 100 baru @ 700.000 = 108 @ 756.000 (rata-rata 7.000)
		SessionStock s = row("8", 56000, "100", 700000, "100");
		BigDecimal diff = StockCalculator.applyClose(s, d("6"), d("4"), d("2")); // tercatat 8, dihitung 6
		assertThat(diff).isEqualByComparingTo("-2");
		assertThat(s.getSoldCost()).isEqualTo(700000);
		assertThat(s.getDiscardedCost()).isEqualTo(14000);
		assertThat(s.getAdjustmentCost()).isEqualTo(14000); // rugi 2 x 7.000
		assertThat(s.getCarriedValue()).isEqualTo(28000);
		assertBalanced(s);
	}

	@Test
	void foundExtraStockIsGainNotLoss() {
		SessionStock s = row("0", 0, "10", 100000, "10"); // habis terjual, tapi ternyata sisa 2
		StockCalculator.applyClose(s, d("2"), d("2"), d("0"));
		assertThat(s.getAdjustmentCost()).isEqualTo(-20000);
		assertThat(s.getCarriedValue()).isEqualTo(20000);
		assertBalanced(s);
	}

	@Test
	void roundingResidueNeverBecomesValueWithoutStockOrNegative() {
		// 3 unit @ 100 rupiah: rata-rata 33,3333; semua terjual -> sisa pembulatan masuk selisih, bukan nilai bawa
		SessionStock s = row("0", 0, "3", 100, "3");
		StockCalculator.applyClose(s, d("0"), d("0"), d("0"));
		assertThat(s.getCarriedValue()).isZero();
		assertBalanced(s);

		// pembulatan naik di tiga pos bisa melebihi total -> carried_value tidak boleh negatif
		SessionStock t = row("0", 0, "3", 200, "1");
		StockCalculator.applyClose(t, d("1"), d("1"), d("0")); // tercatat 2, dihitung 1
		assertThat(t.getCarriedValue()).isGreaterThanOrEqualTo(0);
		assertBalanced(t);
	}

	@Test
	void noOpeningAndNoPurchaseMeansZeroCosts() {
		SessionStock s = row("0", 0, "0", 0, "0");
		StockCalculator.applyClose(s, d("0"), d("0"), d("0"));
		assertThat(s.getSoldCost()).isZero();
		assertThat(s.getCarriedValue()).isZero();
		assertBalanced(s);
	}

	@Test
	void noCostStockHasZeroAverage() {
		SessionStock s = row("0", 0, "50", 0, "20"); // stok dari rumah
		StockCalculator.applyClose(s, d("30"), d("30"), d("0"));
		assertThat(s.getSoldCost()).isZero();
		assertThat(s.getCarriedValue()).isZero();
		assertBalanced(s);
	}

	@Test
	void primaryItemRecordsSoldOutTimeOnceAndSellingNonPrimaryNeverDoes() {
		Instant first = Instant.parse("2026-11-02T08:00:00Z");
		SessionStock primary = row(true, "0", 0, "5", 5000, "0");
		StockCalculator.sell(primary, d("3"), first);
		assertThat(primary.getSoldOutAt()).as("masih ada stok").isNull();
		StockCalculator.sell(primary, d("2"), first);
		assertThat(primary.getSoldOutAt()).isEqualTo(first);
		StockCalculator.sell(primary, d("0"), Instant.parse("2026-11-02T09:00:00Z"));
		assertThat(primary.getSoldOutAt()).as("jam habis tidak ditimpa").isEqualTo(first);

		SessionStock other = row(false, "0", 0, "5", 5000, "0");
		StockCalculator.sell(other, d("5"), first);
		assertThat(other.getSoldOutAt()).isNull();
	}

	@Test
	void soldOutIsClearedWhenStockAppearsAgain() {
		Instant at = Instant.parse("2026-11-02T08:00:00Z");
		SessionStock byPurchase = row(true, "0", 0, "5", 5000, "0");
		StockCalculator.sell(byPurchase, d("5"), at);
		StockCalculator.addStock(byPurchase, d("3"), 3000);
		assertThat(byPurchase.getSoldOutAt()).isNull();

		SessionStock byVoid = row(true, "0", 0, "5", 5000, "0");
		StockCalculator.sell(byVoid, d("5"), at);
		StockCalculator.restore(byVoid, d("2"));
		assertThat(byVoid.getSoldOutAt()).isNull();
		assertThat(StockCalculator.available(byVoid)).isEqualByComparingTo("2");
	}

}
