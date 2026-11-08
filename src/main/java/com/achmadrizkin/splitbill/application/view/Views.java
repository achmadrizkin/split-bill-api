package com.achmadrizkin.splitbill.application.view;

import com.achmadrizkin.splitbill.domain.audit.AuditAction;
import com.achmadrizkin.splitbill.domain.expense.ExpenseCategory;
import com.achmadrizkin.splitbill.domain.expense.ExpenseStatus;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.split.Share;
import com.achmadrizkin.splitbill.domain.split.SplitType;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * Read models returned by the application layer. JPA entities never leave the application layer;
 * the API layer maps these to its own JSON DTOs.
 */
public final class Views {

    private Views() {
    }

    public record GroupView(UUID id, String name, Currency currency, List<ParticipantView> participants,
                            Instant createdAt) {
    }

    public record ParticipantView(UUID id, String name) {
    }

    public record ExpenseView(UUID id, UUID groupId, String description, ExpenseCategory category, Money amount,
                              UUID paidBy, SplitType splitType, ExpenseStatus status, String voidReason,
                              List<Share> shares, Instant createdAt, Instant voidedAt) {
    }

    public record PaymentView(UUID id, UUID groupId, UUID fromParticipantId, UUID toParticipantId, Money amount,
                              String note, Instant createdAt) {
    }

    public record BalanceView(UUID participantId, String name, Money paid, Money share, Money paymentsSent,
                              Money paymentsReceived, Money net) {
    }

    public record TransferView(UUID fromParticipantId, String fromName, UUID toParticipantId, String toName,
                               Money amount) {
    }

    public record CategoryTotalView(ExpenseCategory category, Money total, int expenseCount) {
    }

    public record ServiceChargeShareView(UUID participantId, String name, Money amount) {
    }

    public record SettlementView(UUID groupId,
                                 String groupName,
                                 Currency currency,
                                 Money totalExpenses,
                                 Money totalPayments,
                                 int serviceChargePct,
                                 Money serviceChargeAmount,
                                 List<ServiceChargeShareView> serviceChargeBreakdown,
                                 List<BalanceView> balances,
                                 List<TransferView> transfers,
                                 List<CategoryTotalView> categorySummary,
                                 Instant generatedAt) {
    }

    public record AuditEventView(UUID id, String entityType, UUID entityId, AuditAction action, String details,
                                 Instant occurredAt) {
    }

    /** Result of an idempotent command: {@code replayed} is true when an earlier result was returned. */
    public record IdempotentResult<T>(T value, boolean replayed) {
    }
}
