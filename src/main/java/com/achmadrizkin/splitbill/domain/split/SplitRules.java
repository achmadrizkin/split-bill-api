package com.achmadrizkin.splitbill.domain.split;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.money.Money;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Validation and helpers shared by the split strategies. */
final class SplitRules {

    private SplitRules() {
    }

    static List<UUID> requireUniqueParticipants(List<UUID> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new DomainException(ErrorCode.INVALID_SPLIT, "A split needs at least one participant");
        }
        Set<UUID> seen = new HashSet<>();
        for (UUID id : participantIds) {
            if (id == null) {
                throw new DomainException(ErrorCode.INVALID_SPLIT, "Split participant id is required");
            }
            if (!seen.add(id)) {
                throw new DomainException(ErrorCode.INVALID_SPLIT,
                        "Participant " + id + " appears more than once in the split");
            }
        }
        return List.copyOf(participantIds);
    }

    static List<SplitPortion> requirePositivePortions(List<SplitPortion> portions, String label) {
        if (portions == null || portions.isEmpty()) {
            throw new DomainException(ErrorCode.INVALID_SPLIT, label + " split needs at least one entry");
        }
        requireUniqueParticipants(portions.stream().map(SplitPortion::participantId).toList());
        for (SplitPortion portion : portions) {
            if (portion.value() == null || portion.value().signum() <= 0) {
                throw new DomainException(ErrorCode.INVALID_SPLIT,
                        "%s value for participant %s must be greater than zero".formatted(label, portion.participantId()));
            }
        }
        return List.copyOf(portions);
    }

    static List<UUID> idsOf(List<SplitPortion> portions) {
        return portions.stream().map(SplitPortion::participantId).toList();
    }

    static List<Share> zip(List<UUID> participantIds, List<Money> amounts) {
        List<Share> shares = new ArrayList<>(participantIds.size());
        for (int i = 0; i < participantIds.size(); i++) {
            shares.add(new Share(participantIds.get(i), amounts.get(i)));
        }
        return List.copyOf(shares);
    }
}
