package com.achmadrizkin.splitbill.domain.split;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Raw split input for one participant. The meaning of {@code value} depends on the split type:
 * an amount (EXACT), a percentage (PERCENTAGE), a weight (SHARES) or ignored (EQUAL).
 */
public record SplitPortion(UUID participantId, BigDecimal value) {
}
