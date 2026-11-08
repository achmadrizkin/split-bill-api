package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.money.MoneyAllocator;
import com.achmadrizkin.splitbill.domain.split.EqualSplit;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.achmadrizkin.splitbill.support.Fixtures.IDR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests: instead of a handful of examples, jqwik generates hundreds of random
 * ledgers and checks the invariants a money system must never break.
 */
class SettlementInvariantsTest {

    private static Money units(long minorUnits) {
        return Money.ofMinorUnits(BigInteger.valueOf(minorUnits), IDR);
    }

    @Property(tries = 500)
    void evenAllocationNeverCreatesOrLosesMoney(
            @ForAll @LongRange(min = 0, max = 100_000_000_000_00L) long minorUnits,
            @ForAll @IntRange(min = 1, max = 50) int parts) {

        List<Money> allocation = MoneyAllocator.allocateEvenly(units(minorUnits), parts);

        assertThat(sum(allocation)).isEqualTo(units(minorUnits));
        BigInteger max = allocation.stream().map(Money::toMinorUnits).max(BigInteger::compareTo).orElseThrow();
        BigInteger min = allocation.stream().map(Money::toMinorUnits).min(BigInteger::compareTo).orElseThrow();
        assertThat(max.subtract(min)).isLessThanOrEqualTo(BigInteger.ONE);
    }

    @Property(tries = 500)
    void weightedAllocationNeverCreatesOrLosesMoney(
            @ForAll @LongRange(min = 0, max = 100_000_000_000_00L) long minorUnits,
            @ForAll @Size(min = 1, max = 20) List<@IntRange(min = 1, max = 10_000) Integer> weights) {

        List<BigDecimal> w = weights.stream().map(BigDecimal::valueOf).toList();

        assertThat(sum(MoneyAllocator.allocateByWeights(units(minorUnits), w))).isEqualTo(units(minorUnits));
    }

    @Property(tries = 300)
    void balancesAlwaysNetToZeroAndSettlementClearsThem(
            @ForAll @Size(min = 1, max = 30) List<@LongRange(min = 1, max = 1_000_000_000_00L) Long> expenseAmounts,
            @ForAll @IntRange(min = 2, max = 12) int participantCount) {

        List<UUID> members = IntStream.range(0, participantCount).mapToObj(i -> UUID.randomUUID()).toList();
        List<LedgerExpense> expenses = new ArrayList<>();
        for (int i = 0; i < expenseAmounts.size(); i++) {
            Money amount = units(expenseAmounts.get(i));
            expenses.add(new LedgerExpense(members.get(i % participantCount), amount,
                    new EqualSplit(members).allocate(amount)));
        }

        List<ParticipantBalance> balances = BalanceCalculator.calculate(IDR, members, expenses, List.of());
        assertThat(sum(balances.stream().map(ParticipantBalance::net).toList()).isZero()).isTrue();

        Map<UUID, Money> nets = new LinkedHashMap<>();
        balances.forEach(b -> nets.put(b.participantId(), b.net()));
        assertTransfersClear(nets, SettlementOptimizer.optimize(nets));
    }

    @Property(tries = 500)
    void optimizerClearsAnyZeroSumPositionWithAtMostNMinusOneTransfers(
            @ForAll @Size(min = 1, max = 40) List<@LongRange(min = -1_000_000_000L, max = 1_000_000_000L) Long> positions) {

        Map<UUID, Money> nets = new LinkedHashMap<>();
        long total = 0;
        for (long p : positions) {
            nets.put(UUID.randomUUID(), units(p));
            total += p;
        }
        nets.put(UUID.randomUUID(), units(-total));

        assertTransfersClear(nets, SettlementOptimizer.optimize(nets));
    }

    private static void assertTransfersClear(Map<UUID, Money> nets, List<Transfer> transfers) {
        Map<UUID, Money> remaining = new HashMap<>(nets);
        for (Transfer t : transfers) {
            assertThat(t.amount().isPositive()).isTrue();
            assertThat(t.fromId()).isNotEqualTo(t.toId());
            remaining.merge(t.fromId(), t.amount(), Money::plus);
            remaining.merge(t.toId(), t.amount().negate(), Money::plus);
        }
        assertThat(remaining.values()).allSatisfy(m -> assertThat(m.isZero()).isTrue());
        assertThat(transfers.size()).isLessThanOrEqualTo(Math.max(0, nets.size() - 1));
    }

    private static Money sum(List<Money> values) {
        return values.stream().reduce(Money.zero(IDR), Money::plus);
    }
}
