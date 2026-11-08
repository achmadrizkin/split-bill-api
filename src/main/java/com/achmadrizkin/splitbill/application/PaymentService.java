package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.application.command.RecordPaymentCommand;
import com.achmadrizkin.splitbill.application.view.Views.IdempotentResult;
import com.achmadrizkin.splitbill.application.view.Views.PaymentView;
import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.audit.AuditAction;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.PaymentEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records settle-up payments. A payment may only reduce existing debt: the payer must owe at least
 * the amount and the payee must be owed at least the amount. This prevents a typo (an extra zero)
 * from silently flipping who owes whom.
 */
@Service
public class PaymentService {

    private final GroupLoader groupLoader;
    private final LedgerService ledgerService;
    private final PaymentRepository payments;
    private final IdempotencyService idempotency;
    private final AuditService audit;
    private final Clock clock;

    PaymentService(GroupLoader groupLoader, LedgerService ledgerService, PaymentRepository payments,
                   IdempotencyService idempotency, AuditService audit, Clock clock) {
        this.groupLoader = groupLoader;
        this.ledgerService = ledgerService;
        this.payments = payments;
        this.idempotency = idempotency;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public IdempotentResult<PaymentView> record(UUID groupId, RecordPaymentCommand command, String idempotencyKey) {
        return idempotency.execute("payment:" + groupId, idempotencyKey, command,
                () -> create(groupId, command),
                paymentId -> get(groupId, paymentId));
    }

    private UUID create(UUID groupId, RecordPaymentCommand command) {
        BillGroupEntity group = groupLoader.loadForUpdate(groupId);
        UUID from = command.fromParticipantId();
        UUID to = command.toParticipantId();
        group.requireParticipant(from, "payer");
        group.requireParticipant(to, "payee");
        if (from.equals(to)) {
            throw new DomainException(ErrorCode.INVALID_PAYMENT, "A participant cannot pay themselves");
        }

        Currency currency = group.currencyUnit();
        Money amount = Money.of(command.amount(), currency);
        if (!amount.isPositive()) {
            throw new DomainException(ErrorCode.INVALID_AMOUNT, "Payment amount must be greater than zero");
        }

        Map<UUID, Money> nets = ledgerService.load(group).netBalances();
        Money payerOwes = outstanding(nets.get(from).negate());
        Money payeeIsOwed = outstanding(nets.get(to));
        if (amount.compareTo(payerOwes) > 0) {
            throw new DomainException(ErrorCode.INVALID_PAYMENT,
                    "Payment of %s exceeds what the payer still owes (%s)".formatted(amount, payerOwes));
        }
        if (amount.compareTo(payeeIsOwed) > 0) {
            throw new DomainException(ErrorCode.INVALID_PAYMENT,
                    "Payment of %s exceeds what the payee is still owed (%s)".formatted(amount, payeeIsOwed));
        }

        Instant now = clock.instant();
        String note = command.note() == null ? null : command.note().trim();
        PaymentEntity payment = new PaymentEntity(UUID.randomUUID(), groupId, from, to, amount.amount(), note, now);
        payments.save(payment);
        group.touch(now);

        audit.record(groupId, "PAYMENT", payment.getId(), AuditAction.PAYMENT_RECORDED,
                "from=%s to=%s amount=%s".formatted(from, to, amount));
        return payment.getId();
    }

    @Transactional(readOnly = true)
    public PaymentView get(UUID groupId, UUID paymentId) {
        Currency currency = groupLoader.load(groupId).currencyUnit();
        return payments.findByIdAndGroupId(paymentId, groupId)
                .map(p -> ViewMapper.payment(p, currency))
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment " + paymentId + " does not exist in group " + groupId));
    }

    @Transactional(readOnly = true)
    public List<PaymentView> list(UUID groupId) {
        Currency currency = groupLoader.load(groupId).currencyUnit();
        return payments.findByGroupIdOrderByCreatedAtAscIdAsc(groupId).stream()
                .map(p -> ViewMapper.payment(p, currency))
                .toList();
    }

    private static Money outstanding(Money value) {
        return value.isPositive() ? value : Money.zero(value.currency());
    }
}
