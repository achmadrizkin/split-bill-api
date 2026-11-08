package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.application.view.Views.BalanceView;
import com.achmadrizkin.splitbill.application.view.Views.CategoryTotalView;
import com.achmadrizkin.splitbill.application.view.Views.ServiceChargeShareView;
import com.achmadrizkin.splitbill.application.view.Views.SettlementView;
import com.achmadrizkin.splitbill.application.view.Views.TransferView;
import com.achmadrizkin.splitbill.domain.charge.ServiceChargePolicy;
import com.achmadrizkin.splitbill.domain.expense.ExpenseCategory;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.money.MoneyAllocator;
import com.achmadrizkin.splitbill.domain.settlement.ParticipantBalance;
import com.achmadrizkin.splitbill.domain.settlement.SettlementOptimizer;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.ExpenseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read side: balances, optimized transfers, category summary and the personalized service charge. */
@Service
public class SettlementService {

    private final GroupLoader groupLoader;
    private final LedgerService ledgerService;
    private final ServiceChargePolicy serviceChargePolicy;
    private final Clock clock;

    SettlementService(GroupLoader groupLoader, LedgerService ledgerService, ServiceChargePolicy serviceChargePolicy,
                      Clock clock) {
        this.groupLoader = groupLoader;
        this.ledgerService = ledgerService;
        this.serviceChargePolicy = serviceChargePolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BalanceView> balances(UUID groupId) {
        BillGroupEntity group = groupLoader.load(groupId);
        return toBalanceViews(ledgerService.load(group).balances(), group.participantNames());
    }

    @Transactional(readOnly = true)
    public SettlementView settle(UUID groupId) {
        BillGroupEntity group = groupLoader.load(groupId);
        Ledger ledger = ledgerService.load(group);
        Map<UUID, String> names = group.participantNames();

        Money totalExpenses = ledger.totalExpenses();
        Money serviceCharge = serviceChargePolicy.chargeFor(totalExpenses);

        List<TransferView> transfers = SettlementOptimizer.optimize(ledger.netBalances()).stream()
                .map(t -> new TransferView(t.fromId(), names.get(t.fromId()), t.toId(), names.get(t.toId()),
                        t.amount()))
                .toList();

        return new SettlementView(
                group.getId(),
                group.getName(),
                ledger.currency(),
                totalExpenses,
                ledger.totalPayments(),
                serviceChargePolicy.percentage(),
                serviceCharge,
                serviceChargeBreakdown(ledger.balances(), serviceCharge, names),
                toBalanceViews(ledger.balances(), names),
                transfers,
                categoryTotals(ledger),
                clock.instant());
    }

    /**
     * Informational: how the service charge would be borne if passed on, proportional to each
     * participant's share of expenses. Allocated with the largest-remainder method so it sums exactly.
     */
    private static List<ServiceChargeShareView> serviceChargeBreakdown(List<ParticipantBalance> balances,
                                                                       Money charge, Map<UUID, String> names) {
        if (balances.isEmpty()) {
            return List.of();
        }
        List<BigDecimal> weights = balances.stream().map(b -> b.share().amount()).toList();
        boolean hasWeight = weights.stream().anyMatch(w -> w.signum() > 0);

        List<ServiceChargeShareView> breakdown = new ArrayList<>(balances.size());
        List<Money> amounts = charge.isPositive() && hasWeight
                ? MoneyAllocator.allocateByWeights(charge, weights)
                : balances.stream().map(b -> Money.zero(charge.currency())).toList();
        for (int i = 0; i < balances.size(); i++) {
            UUID id = balances.get(i).participantId();
            breakdown.add(new ServiceChargeShareView(id, names.get(id), amounts.get(i)));
        }
        return breakdown;
    }

    private static List<BalanceView> toBalanceViews(List<ParticipantBalance> balances, Map<UUID, String> names) {
        return balances.stream()
                .map(b -> new BalanceView(b.participantId(), names.get(b.participantId()), b.paid(), b.share(),
                        b.paymentsSent(), b.paymentsReceived(), b.net()))
                .toList();
    }

    private static List<CategoryTotalView> categoryTotals(Ledger ledger) {
        Map<ExpenseCategory, Money> totals = new EnumMap<>(ExpenseCategory.class);
        Map<ExpenseCategory, Integer> counts = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseEntity expense : ledger.activeExpenses()) {
            totals.merge(expense.getCategory(), Money.of(expense.getAmount(), ledger.currency()), Money::plus);
            counts.merge(expense.getCategory(), 1, Integer::sum);
        }
        return totals.entrySet().stream()
                .map(e -> new CategoryTotalView(e.getKey(), e.getValue(), counts.get(e.getKey())))
                .sorted(Comparator.comparing(CategoryTotalView::total).reversed())
                .toList();
    }
}
