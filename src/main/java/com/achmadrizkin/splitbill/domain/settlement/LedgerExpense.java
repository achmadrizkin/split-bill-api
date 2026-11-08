package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.split.Share;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** An expense as seen by the ledger: who paid, how much, and who owes which part. */
public record LedgerExpense(UUID payerId, Money amount, List<Share> shares) {

    public LedgerExpense {
        Objects.requireNonNull(payerId, "payerId");
        Objects.requireNonNull(amount, "amount");
        shares = List.copyOf(shares);
        Money sum = shares.stream().map(Share::amount).reduce(Money.zero(amount.currency()), Money::plus);
        if (sum.compareTo(amount) != 0) {
            throw new IllegalArgumentException("Shares add up to " + sum + " but expense amount is " + amount);
        }
    }
}
