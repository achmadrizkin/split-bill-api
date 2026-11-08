package com.achmadrizkin.splitbill.domain.money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Splits an amount into parts without creating or losing a single minor unit,
 * using the <b>largest remainder method</b> (Hamilton's method):
 *
 * <ol>
 *   <li>Work in minor units (cents) so everything is an integer.</li>
 *   <li>Give every part the floor of its exact proportional value.</li>
 *   <li>Hand out the few leftover units, one each, to the parts with the largest
 *       fractional remainder (ties: earlier position first, so results are deterministic).</li>
 * </ol>
 *
 * Invariant: {@code sum(result) == total}, always.
 */
public final class MoneyAllocator {

    private MoneyAllocator() {
    }

    public static List<Money> allocateEvenly(Money total, int parts) {
        if (parts < 1) {
            throw new IllegalArgumentException("parts must be at least 1");
        }
        return allocateByWeights(total, Collections.nCopies(parts, BigDecimal.ONE));
    }

    public static List<Money> allocateByWeights(Money total, List<BigDecimal> weights) {
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(weights, "weights");
        if (total.isNegative()) {
            throw new IllegalArgumentException("Cannot allocate a negative amount");
        }
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("At least one weight is required");
        }
        BigDecimal weightSum = BigDecimal.ZERO;
        for (BigDecimal weight : weights) {
            if (weight == null || weight.signum() < 0) {
                throw new IllegalArgumentException("Weights must be non-negative");
            }
            weightSum = weightSum.add(weight);
        }
        if (weightSum.signum() == 0) {
            throw new IllegalArgumentException("At least one weight must be positive");
        }

        BigInteger totalUnits = total.toMinorUnits();
        BigDecimal units = new BigDecimal(totalUnits);
        int n = weights.size();
        BigInteger[] allocated = new BigInteger[n];
        BigDecimal[] remainders = new BigDecimal[n];
        BigInteger distributed = BigInteger.ZERO;

        for (int i = 0; i < n; i++) {
            BigDecimal[] quotientAndRemainder = units.multiply(weights.get(i)).divideAndRemainder(weightSum);
            allocated[i] = quotientAndRemainder[0].toBigIntegerExact();
            remainders[i] = quotientAndRemainder[1];
            distributed = distributed.add(allocated[i]);
        }

        int leftover = totalUnits.subtract(distributed).intValueExact(); // always 0 <= leftover < n
        List<Integer> byLargestRemainder = IntStream.range(0, n).boxed()
                .sorted(Comparator.comparing((Integer i) -> remainders[i]).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
        for (int k = 0; k < leftover; k++) {
            int index = byLargestRemainder.get(k);
            allocated[index] = allocated[index].add(BigInteger.ONE);
        }

        List<Money> result = new ArrayList<>(n);
        for (BigInteger part : allocated) {
            result.add(Money.ofMinorUnits(part, total.currency()));
        }
        return List.copyOf(result);
    }
}
