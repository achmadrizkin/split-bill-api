package com.achmadrizkin.splitbill.domain.split;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.money.MoneyAllocator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SharesSplit(List<SplitPortion> portions) implements SplitStrategy {

    public SharesSplit {
        portions = SplitRules.requirePositivePortions(portions, "Share weight");
        for (SplitPortion portion : portions) {
            if (portion.value().stripTrailingZeros().scale() > 0) {
                throw new DomainException(ErrorCode.INVALID_SPLIT,
                        "Share weight for participant %s must be a whole number".formatted(portion.participantId()));
            }
        }
    }

    @Override
    public SplitType type() {
        return SplitType.SHARES;
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
