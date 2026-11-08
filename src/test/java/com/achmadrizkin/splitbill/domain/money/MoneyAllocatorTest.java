package com.achmadrizkin.splitbill.domain.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.achmadrizkin.splitbill.support.Fixtures.JPY;
import static com.achmadrizkin.splitbill.support.Fixtures.idr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MoneyAllocatorTest {

    @Test
    void evenSplitGivesTheLeftoverCentToTheFirstParticipantSoNothingIsLost() {
        List<Money> parts = MoneyAllocator.allocateEvenly(idr("100000"), 3);

        assertThat(parts).containsExactly(idr("33333.34"), idr("33333.33"), idr("33333.33"));
        assertThat(sum(parts)).isEqualTo(idr("100000"));
    }

    @Test
    void smallestUnitCannotBeSplitFurther() {
        assertThat(MoneyAllocator.allocateEvenly(idr("0.01"), 3))
                .containsExactly(idr("0.01"), idr("0.00"), idr("0.00"));
    }

    @Test
    void respectsCurrenciesWithoutMinorUnits() {
        Money yen = Money.of(new BigDecimal("100"), JPY);
        assertThat(MoneyAllocator.allocateEvenly(yen, 3)).extracting(m -> m.amount().toPlainString())
                .containsExactly("34", "33", "33");
    }

    @Test
    void weightedAllocationGivesLeftoverToLargestFractionalRemainder() {
        // 10.00 * 33.33% = 3.333, 10.00 * 33.34% = 3.334 -> the 0.01 goes to the third person
        List<Money> parts = MoneyAllocator.allocateByWeights(idr("10"),
                List.of(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34")));

        assertThat(parts).containsExactly(idr("3.33"), idr("3.33"), idr("3.34"));
    }

    @Test
    void exactRatiosAreNotDisturbed() {
        assertThat(MoneyAllocator.allocateByWeights(idr("100"),
                List.of(BigDecimal.valueOf(2), BigDecimal.ONE, BigDecimal.ONE)))
                .containsExactly(idr("50"), idr("25"), idr("25"));
    }

    @Test
    void zeroWeightNeverReceivesLeftoverUnits() {
        assertThat(MoneyAllocator.allocateByWeights(idr("0.05"),
                List.of(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE)))
                .containsExactly(idr("0.03"), idr("0.00"), idr("0.02"));
    }

    @Test
    void rejectsInvalidInput() {
        assertThatIllegalArgumentException().isThrownBy(() -> MoneyAllocator.allocateEvenly(idr("1"), 0));
        assertThatIllegalArgumentException().isThrownBy(() -> MoneyAllocator.allocateByWeights(idr("1"), List.of()));
        assertThatIllegalArgumentException().isThrownBy(() ->
                MoneyAllocator.allocateByWeights(idr("1"), List.of(BigDecimal.ONE, BigDecimal.ONE.negate())));
        assertThatIllegalArgumentException().isThrownBy(() ->
                MoneyAllocator.allocateByWeights(idr("1"), List.of(BigDecimal.ZERO)));
        assertThatIllegalArgumentException().isThrownBy(() -> MoneyAllocator.allocateEvenly(idr("-1"), 2));
    }

    private static Money sum(List<Money> parts) {
        return parts.stream().reduce(Money::plus).orElseThrow();
    }
}
