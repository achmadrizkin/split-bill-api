package com.achmadrizkin.splitbill.domain.split;

import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.money.MoneyAllocator;

import java.util.List;
import java.util.UUID;

public record EqualSplit(List<UUID> participantIds) implements SplitStrategy {

    public EqualSplit {
        participantIds = SplitRules.requireUniqueParticipants(participantIds);
    }

    @Override
    public SplitType type() {
        return SplitType.EQUAL;
    }

    @Override
    public List<Share> allocate(Money total) {
        return SplitRules.zip(participantIds, MoneyAllocator.allocateEvenly(total, participantIds.size()));
    }
}
