package com.achmadrizkin.splitbill.domain.split;

import java.util.List;
import java.util.UUID;

/** Builds a {@link SplitStrategy} from raw request input. */
public final class SplitStrategies {

    private SplitStrategies() {
    }

    /**
     * @param type                requested split type; {@code null} means EQUAL
     * @param entries             per-participant input; may be empty for EQUAL
     * @param defaultParticipants used for an EQUAL split without entries (typically the whole group)
     */
    public static SplitStrategy of(SplitType type, List<SplitPortion> entries, List<UUID> defaultParticipants) {
        SplitType effectiveType = type == null ? SplitType.EQUAL : type;
        List<SplitPortion> safeEntries = entries == null ? List.of() : entries;
        return switch (effectiveType) {
            case EQUAL -> new EqualSplit(safeEntries.isEmpty()
                    ? defaultParticipants
                    : SplitRules.idsOf(safeEntries));
            case EXACT -> new ExactSplit(safeEntries);
            case PERCENTAGE -> new PercentageSplit(safeEntries);
            case SHARES -> new SharesSplit(safeEntries);
        };
    }
}
