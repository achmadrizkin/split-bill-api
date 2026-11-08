package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.money.Money;

import java.util.UUID;

/** A suggested settlement payment: {@code fromId} pays {@code toId} the given amount. */
public record Transfer(UUID fromId, UUID toId, Money amount) {
}
