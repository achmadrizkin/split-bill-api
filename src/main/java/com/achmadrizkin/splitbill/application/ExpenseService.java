package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.application.command.AddExpenseCommand;
import com.achmadrizkin.splitbill.application.view.Views.ExpenseView;
import com.achmadrizkin.splitbill.application.view.Views.IdempotentResult;
import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.audit.AuditAction;
import com.achmadrizkin.splitbill.domain.expense.ExpenseCategory;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.split.Share;
import com.achmadrizkin.splitbill.domain.split.SplitStrategies;
import com.achmadrizkin.splitbill.domain.split.SplitStrategy;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.ExpenseEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.ExpenseRepository;
import com.achmadrizkin.splitbill.infrastructure.persistence.ExpenseShareEmbeddable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final GroupLoader groupLoader;
    private final ExpenseRepository expenses;
    private final IdempotencyService idempotency;
    private final AuditService audit;
    private final Clock clock;

    ExpenseService(GroupLoader groupLoader, ExpenseRepository expenses, IdempotencyService idempotency,
                   AuditService audit, Clock clock) {
        this.groupLoader = groupLoader;
        this.expenses = expenses;
        this.idempotency = idempotency;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public IdempotentResult<ExpenseView> add(UUID groupId, AddExpenseCommand command, String idempotencyKey) {
        return idempotency.execute("expense:" + groupId, idempotencyKey, command,
                () -> create(groupId, command),
                expenseId -> get(groupId, expenseId));
    }

    private UUID create(UUID groupId, AddExpenseCommand command) {
        BillGroupEntity group = groupLoader.loadForUpdate(groupId);

        Money total = Money.of(command.amount(), group.currencyUnit());
        if (!total.isPositive()) {
            throw new DomainException(ErrorCode.INVALID_AMOUNT, "Expense amount must be greater than zero");
        }
        group.requireParticipant(command.paidBy(), "payer");

        SplitStrategy split = SplitStrategies.of(command.splitType(), command.splits(), group.participantIds());
        split.participantIds().forEach(id -> group.requireParticipant(id, "split participant"));
        List<Share> shares = split.allocate(total);

        Instant now = clock.instant();
        ExpenseCategory category = command.category() == null ? ExpenseCategory.OTHER : command.category();
        ExpenseEntity expense = new ExpenseEntity(UUID.randomUUID(), groupId, command.description().trim(), category,
                total.amount(), command.paidBy(), split.type(),
                shares.stream().map(s -> new ExpenseShareEmbeddable(s.participantId(), s.amount().amount())).toList(),
                now);
        expenses.save(expense);
        group.touch(now);

        audit.record(groupId, "EXPENSE", expense.getId(), AuditAction.EXPENSE_ADDED,
                "description=%s amount=%s paid_by=%s split=%s".formatted(
                        expense.getDescription(), total, command.paidBy(), split.type()));
        return expense.getId();
    }

    @Transactional(readOnly = true)
    public ExpenseView get(UUID groupId, UUID expenseId) {
        BillGroupEntity group = groupLoader.load(groupId);
        return ViewMapper.expense(find(groupId, expenseId), group.currencyUnit());
    }

    @Transactional(readOnly = true)
    public List<ExpenseView> list(UUID groupId, ExpenseCategory category) {
        Currency currency = groupLoader.load(groupId).currencyUnit();
        return expenses.findByGroupIdOrderByCreatedAtAscIdAsc(groupId).stream()
                .filter(e -> category == null || e.getCategory() == category)
                .map(e -> ViewMapper.expense(e, currency))
                .toList();
    }

    /**
     * Expenses are never deleted. Voiding keeps the record (and its audit trail) but removes it
     * from balances, exactly like a reversal in a bank ledger.
     */
    @Transactional
    public ExpenseView voidExpense(UUID groupId, UUID expenseId, String reason) {
        BillGroupEntity group = groupLoader.loadForUpdate(groupId);
        ExpenseEntity expense = find(groupId, expenseId);
        Instant now = clock.instant();

        expense.voidExpense(reason.trim(), now);
        group.touch(now);

        audit.record(groupId, "EXPENSE", expenseId, AuditAction.EXPENSE_VOIDED, "reason=" + reason.trim());
        return ViewMapper.expense(expense, group.currencyUnit());
    }

    private ExpenseEntity find(UUID groupId, UUID expenseId) {
        return expenses.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new DomainException(ErrorCode.EXPENSE_NOT_FOUND,
                        "Expense " + expenseId + " does not exist in group " + groupId));
    }
}
