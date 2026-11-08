package com.achmadrizkin.splitbill.domain.split;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.money.Money;

import java.util.List;
import java.util.UUID;

public record ExactSplit(List<SplitPortion> portions) implements SplitStrategy {

    public ExactSplit {
        portions = SplitRules.requirePositivePortions(portions, "Exact amount");
    }

    @Override
    public SplitType type() {
        return SplitType.EXACT;
    }

    @Override
    public List<UUID> participantIds() {
        return SplitRules.idsOf(portions);
    }

    @Override
    public List<Share> allocate(Money total) {
        List<Share> shares = portions.stream()
                .map(p -> new Share(p.participantId(), Money.of(p.value(), total.currency())))
                .toList();
        Money sum = shares.stream().map(Share::amount).reduce(Money.zero(total.currency()), Money::plus);
        if (sum.compareTo(total) != 0) {
            throw new DomainException(ErrorCode.SPLIT_SUM_MISMATCH,
                    "Exact split amounts add up to %s but the expense total is %s".formatted(sum, total));
        }
        return shares;
    }
}
