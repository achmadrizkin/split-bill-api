package com.achmadrizkin.splitbill.domain.split;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.money.MoneyAllocator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PercentageSplit(List<SplitPortion> portions) implements SplitStrategy {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public PercentageSplit {
        portions = SplitRules.requirePositivePortions(portions, "Percentage");
        BigDecimal sum = portions.stream().map(SplitPortion::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(ONE_HUNDRED) != 0) {
            throw new DomainException(ErrorCode.SPLIT_SUM_MISMATCH,
                    "Percentages add up to " + sum.toPlainString() + " but must add up to exactly 100");
        }
    }

    @Override
    public SplitType type() {
        return SplitType.PERCENTAGE;
    }

    @Override
    public List<UUID> participantIds() {
        return SplitRules.idsOf(portions);
    }

    @Override
    public List<Share> allocate(Money total) {
        List<BigDecimal> weights = portions.stream().map(SplitPortion::value).toList();
        return SplitRules.zip(participantIds(), MoneyAllocator.allocateByWeights(total, weights));
    }
}
