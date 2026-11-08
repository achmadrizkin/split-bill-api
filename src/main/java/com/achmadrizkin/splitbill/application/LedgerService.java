package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.settlement.BalanceCalculator;
import com.achmadrizkin.splitbill.domain.settlement.LedgerExpense;
import com.achmadrizkin.splitbill.domain.settlement.LedgerPayment;
import com.achmadrizkin.splitbill.domain.split.Share;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.ExpenseEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.ExpenseRepository;
import com.achmadrizkin.splitbill.infrastructure.persistence.PaymentEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.List;

/**
 * Loads a group's ledger and hands it to the pure domain calculator.
 * Must be called inside a transaction owned by the calling service.
 */
@Component
class LedgerService {

    private final ExpenseRepository expenses;
    private final PaymentRepository payments;

    LedgerService(ExpenseRepository expenses, PaymentRepository payments) {
        this.expenses = expenses;
        this.payments = payments;
    }

    Ledger load(BillGroupEntity group) {
        Currency currency = group.currencyUnit();
        List<ExpenseEntity> activeExpenses = expenses.findByGroupIdOrderByCreatedAtAscIdAsc(group.getId()).stream()
                .filter(ExpenseEntity::isActive)
                .toList();
        List<PaymentEntity> groupPayments = payments.findByGroupIdOrderByCreatedAtAscIdAsc(group.getId());

        List<LedgerExpense> ledgerExpenses = activeExpenses.stream()
                .map(e -> new LedgerExpense(e.getPayerId(), Money.of(e.getAmount(), currency),
                        e.getShares().stream()
                                .map(s -> new Share(s.getParticipantId(), Money.of(s.getAmount(), currency)))
                                .toList()))
                .toList();
        List<LedgerPayment> ledgerPayments = groupPayments.stream()
                .map(p -> new LedgerPayment(p.getFromId(), p.getToId(), Money.of(p.getAmount(), currency)))
                .toList();

        return new Ledger(currency, activeExpenses, groupPayments,
                BalanceCalculator.calculate(currency, group.participantIds(), ledgerExpenses, ledgerPayments));
    }
}
