package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.split.Share;

import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Derives every participant's balance from the ledger (expenses + payments).
 * Balances are never stored; like a core-banking system, balance = sum of journal entries.
 */
public final class BalanceCalculator {

    private BalanceCalculator() {
    }

    public static List<ParticipantBalance> calculate(Currency currency,
                                                     List<UUID> participantIds,
                                                     List<LedgerExpense> expenses,
                                                     List<LedgerPayment> payments) {
        Map<UUID, Account> accounts = new LinkedHashMap<>();
        participantIds.forEach(id -> accounts.put(id, new Account(currency)));

        for (LedgerExpense expense : expenses) {
            Account payer = accountOf(accounts, expense.payerId());
            payer.paid = payer.paid.plus(expense.amount());
            for (Share share : expense.shares()) {
                Account debtor = accountOf(accounts, share.participantId());
                debtor.share = debtor.share.plus(share.amount());
            }
        }
        for (LedgerPayment payment : payments) {
            Account from = accountOf(accounts, payment.fromId());
            Account to = accountOf(accounts, payment.toId());
            from.sent = from.sent.plus(payment.amount());
            to.received = to.received.plus(payment.amount());
        }

        List<ParticipantBalance> balances = accounts.entrySet().stream()
                .map(e -> e.getValue().toBalance(e.getKey()))
                .toList();

        Money total = balances.stream().map(ParticipantBalance::net).reduce(Money.zero(currency), Money::plus);
        if (!total.isZero()) {
            // Cannot happen if every LedgerExpense is valid; fail loudly rather than report wrong numbers.
            throw new IllegalStateException("Ledger is unbalanced: net positions sum to " + total);
        }
        return balances;
    }

    private static Account accountOf(Map<UUID, Account> accounts, UUID participantId) {
        Account account = accounts.get(participantId);
        if (account == null) {
            throw new DomainException(ErrorCode.PARTICIPANT_NOT_IN_GROUP,
                    "Participant " + participantId + " is not part of this group");
        }
        return account;
    }

    private static final class Account {
        private Money paid;
        private Money share;
        private Money sent;
        private Money received;

        private Account(Currency currency) {
            Money zero = Money.zero(currency);
            this.paid = zero;
            this.share = zero;
            this.sent = zero;
            this.received = zero;
        }

        private ParticipantBalance toBalance(UUID participantId) {
            return new ParticipantBalance(participantId, paid, share, sent, received);
        }
    }
}
