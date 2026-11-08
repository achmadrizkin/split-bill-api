package com.achmadrizkin.splitbill.domain.split;

import com.achmadrizkin.splitbill.domain.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.achmadrizkin.splitbill.support.Fixtures.assertDomainError;
import static com.achmadrizkin.splitbill.support.Fixtures.idr;
import static org.assertj.core.api.Assertions.assertThat;

class SplitStrategyTest {

    private static final UUID ANDI = UUID.randomUUID();
    private static final UUID BUDI = UUID.randomUUID();
    private static final UUID CITRA = UUID.randomUUID();

    private static SplitPortion portion(UUID id, String value) {
        return new SplitPortion(id, new BigDecimal(value));
    }

    @Nested
    class Equal {

        @Test
        void splitsEvenlyInInputOrder() {
            List<Share> shares = new EqualSplit(List.of(ANDI, BUDI, CITRA)).allocate(idr("100000"));

            assertThat(shares).containsExactly(
                    new Share(ANDI, idr("33333.34")),
                    new Share(BUDI, idr("33333.33")),
                    new Share(CITRA, idr("33333.33")));
        }

        @Test
        void requiresAtLeastOneUniqueParticipant() {
            assertDomainError(() -> new EqualSplit(List.of()), ErrorCode.INVALID_SPLIT);
            assertDomainError(() -> new EqualSplit(List.of(ANDI, ANDI)), ErrorCode.INVALID_SPLIT);
        }
    }

    @Nested
    class Exact {

        @Test
        void usesTheGivenAmounts() {
            List<Share> shares = new ExactSplit(List.of(portion(ANDI, "50000"), portion(BUDI, "50000")))
                    .allocate(idr("100000"));

            assertThat(shares).containsExactly(new Share(ANDI, idr("50000")), new Share(BUDI, idr("50000")));
        }

        @Test
        void amountsMustAddUpToTheExpenseTotal() {
            ExactSplit split = new ExactSplit(List.of(portion(ANDI, "50000"), portion(BUDI, "40000")));
            assertDomainError(() -> split.allocate(idr("100000")), ErrorCode.SPLIT_SUM_MISMATCH);
        }

        @Test
        void rejectsNonPositiveOrOverPreciseAmounts() {
            assertDomainError(() -> new ExactSplit(List.of(portion(ANDI, "0"))), ErrorCode.INVALID_SPLIT);
            assertDomainError(() -> new ExactSplit(List.of(portion(ANDI, "-1"))), ErrorCode.INVALID_SPLIT);
            assertDomainError(() -> new ExactSplit(List.of(portion(ANDI, "0.001"))).allocate(idr("1")),
                    ErrorCode.AMOUNT_SCALE_EXCEEDED);
        }
    }

    @Nested
    class Percentage {

        @Test
        void allocatesByPercentageWithoutLosingCents() {
            List<Share> shares = new PercentageSplit(List.of(
                    portion(ANDI, "33.33"), portion(BUDI, "33.33"), portion(CITRA, "33.34")))
                    .allocate(idr("10"));

            assertThat(shares).extracting(Share::amount).containsExactly(idr("3.33"), idr("3.33"), idr("3.34"));
        }

        @Test
        void percentagesMustAddUpToExactlyOneHundred() {
            assertDomainError(() -> new PercentageSplit(List.of(portion(ANDI, "50"), portion(BUDI, "49.99"))),
                    ErrorCode.SPLIT_SUM_MISMATCH);
        }
    }

    @Nested
    class Shares {

        @Test
        void allocatesByWeight() {
            List<Share> shares = new SharesSplit(List.of(portion(ANDI, "2"), portion(BUDI, "1"), portion(CITRA, "1")))
                    .allocate(idr("100000"));

            assertThat(shares).extracting(Share::amount)
                    .containsExactly(idr("50000"), idr("25000"), idr("25000"));
        }

        @Test
        void weightsMustBePositiveWholeNumbers() {
            assertDomainError(() -> new SharesSplit(List.of(portion(ANDI, "1.5"))), ErrorCode.INVALID_SPLIT);
            assertDomainError(() -> new SharesSplit(List.of(portion(ANDI, "0"))), ErrorCode.INVALID_SPLIT);
        }
    }

    @Nested
    class Factory {

        @Test
        void equalSplitWithoutEntriesDefaultsToEveryone() {
            SplitStrategy split = SplitStrategies.of(null, null, List.of(ANDI, BUDI));

            assertThat(split).isInstanceOf(EqualSplit.class);
            assertThat(split.participantIds()).containsExactly(ANDI, BUDI);
        }

        @Test
        void nonEqualSplitsRequireEntries() {
            assertDomainError(() -> SplitStrategies.of(SplitType.EXACT, List.of(), List.of(ANDI)),
                    ErrorCode.INVALID_SPLIT);
        }

        @Test
        void buildsTheRequestedStrategy() {
            List<SplitPortion> entries = List.of(portion(ANDI, "60"), portion(BUDI, "40"));

            assertThat(SplitStrategies.of(SplitType.PERCENTAGE, entries, List.of())).isInstanceOf(PercentageSplit.class);
            assertThat(SplitStrategies.of(SplitType.EXACT, entries, List.of())).isInstanceOf(ExactSplit.class);
            assertThat(SplitStrategies.of(SplitType.SHARES, entries, List.of())).isInstanceOf(SharesSplit.class);
        }
    }
}
