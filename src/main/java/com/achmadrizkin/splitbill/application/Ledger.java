package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.settlement.ParticipantBalance;
import com.achmadrizkin.splitbill.infrastructure.persistence.ExpenseEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.PaymentEntity;

import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A consistent snapshot of a group's active expenses, payments and the balances derived from them. */
record Ledger(Currency currency,
              List<ExpenseEntity> activeExpenses,
              List<PaymentEntity> payments,
              List<ParticipantBalance> balances) {

    Map<UUID, Money> netBalances() {
        Map<UUID, Money> nets = new LinkedHashMap<>();
        balances.forEach(b -> nets.put(b.participantId(), b.net()));
        return nets;
    }

    Money totalExpenses() {
        return activeExpenses.stream()
                .map(e -> Money.of(e.getAmount(), currency))
                .reduce(Money.zero(currency), Money::plus);
    }

    Money totalPayments() {
        return payments.stream()
                .map(p -> Money.of(p.getAmount(), currency))
                .reduce(Money.zero(currency), Money::plus);
    }
}
