package com.achmadrizkin.splitbill.domain.split;

import com.achmadrizkin.splitbill.domain.money.Money;

import java.util.List;
import java.util.UUID;

/**
 * How an expense total is divided among participants.
 * Sealed so the compiler guarantees every strategy is handled in {@code switch} expressions.
 *
 * <p>Contract: {@code allocate(total)} returns shares in participant order whose sum equals {@code total} exactly.
 */
public sealed interface SplitStrategy permits EqualSplit, ExactSplit, PercentageSplit, SharesSplit {

    SplitType type();

    List<UUID> participantIds();

    List<Share> allocate(Money total);
}
