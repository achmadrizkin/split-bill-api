package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.money.Money;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Turns net balances into a short list of transfers using the greedy
 * <b>min-cash-flow</b> heuristic: repeatedly match the largest debtor with the largest creditor.
 *
 * <ul>
 *   <li>Guarantees at most {@code n - 1} transfers and that every balance ends at zero.</li>
 *   <li>Runs in O(n log n).</li>
 *   <li>Trade-off: the true minimum is NP-hard (partitioning into zero-sum subsets);
 *       greedy is optimal or near-optimal for realistic group sizes.</li>
 *   <li>Deterministic: ties are broken by the iteration order of the input map.</li>
 * </ul>
 */
public final class SettlementOptimizer {

    private SettlementOptimizer() {
    }

    public static List<Transfer> optimize(Map<UUID, Money> netBalances) {
        if (netBalances.isEmpty()) {
            return List.of();
        }
        Currency currency = netBalances.values().iterator().next().currency();
        Money sum = netBalances.values().stream().reduce(Money.zero(currency), Money::plus);
        if (!sum.isZero()) {
            throw new IllegalArgumentException("Net balances must sum to zero but sum to " + sum);
        }

        Comparator<Position> largestFirst = Comparator.comparing(Position::units).reversed()
                .thenComparingInt(Position::order);
        PriorityQueue<Position> debtors = new PriorityQueue<>(largestFirst);
        PriorityQueue<Position> creditors = new PriorityQueue<>(largestFirst);

        int order = 0;
        for (Map.Entry<UUID, Money> entry : netBalances.entrySet()) {
            BigInteger units = entry.getValue().toMinorUnits();
            if (units.signum() < 0) {
                debtors.add(new Position(entry.getKey(), units.negate(), order));
            } else if (units.signum() > 0) {
                creditors.add(new Position(entry.getKey(), units, order));
            }
            order++;
        }

        List<Transfer> transfers = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Position debtor = debtors.poll();
            Position creditor = creditors.poll();
            BigInteger amount = debtor.units().min(creditor.units());

            transfers.add(new Transfer(debtor.participantId(), creditor.participantId(),
                    Money.ofMinorUnits(amount, currency)));

            if (debtor.units().compareTo(amount) > 0) {
                debtors.add(debtor.reduceBy(amount));
            }
            if (creditor.units().compareTo(amount) > 0) {
                creditors.add(creditor.reduceBy(amount));
            }
        }
        return List.copyOf(transfers);
    }

    /** Outstanding absolute amount (minor units) for one participant. */
    private record Position(UUID participantId, BigInteger units, int order) {
        Position reduceBy(BigInteger amount) {
            return new Position(participantId, units.subtract(amount), order);
        }
    }
}
