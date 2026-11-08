package com.achmadrizkin.splitbill.domain.split;

import com.achmadrizkin.splitbill.domain.money.Money;

import java.util.Objects;
import java.util.UUID;

/** The part of an expense a participant is responsible for. */
public record Share(UUID participantId, Money amount) {

    public Share {
        Objects.requireNonNull(participantId, "participantId");
        Objects.requireNonNull(amount, "amount");
    }
}
