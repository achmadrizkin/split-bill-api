package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.money.Money;

import java.util.Objects;
import java.util.UUID;

/** Money that already changed hands between two participants (a settle-up). */
public record LedgerPayment(UUID fromId, UUID toId, Money amount) {

    public LedgerPayment {
        Objects.requireNonNull(fromId, "fromId");
        Objects.requireNonNull(toId, "toId");
        Objects.requireNonNull(amount, "amount");
    }
}
