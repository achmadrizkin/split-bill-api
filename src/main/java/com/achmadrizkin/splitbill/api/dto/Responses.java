package com.achmadrizkin.splitbill.api.dto;

import com.achmadrizkin.splitbill.application.view.Views.AuditEventView;
import com.achmadrizkin.splitbill.application.view.Views.BalanceView;
import com.achmadrizkin.splitbill.application.view.Views.CategoryTotalView;
import com.achmadrizkin.splitbill.application.view.Views.ExpenseView;
import com.achmadrizkin.splitbill.application.view.Views.GroupView;
import com.achmadrizkin.splitbill.application.view.Views.ParticipantView;
import com.achmadrizkin.splitbill.application.view.Views.PaymentView;
import com.achmadrizkin.splitbill.application.view.Views.ServiceChargeShareView;
import com.achmadrizkin.splitbill.application.view.Views.SettlementView;
import com.achmadrizkin.splitbill.application.view.Views.TransferView;
import com.achmadrizkin.splitbill.domain.audit.AuditAction;
import com.achmadrizkin.splitbill.domain.expense.ExpenseCategory;
import com.achmadrizkin.splitbill.domain.expense.ExpenseStatus;
import com.achmadrizkin.splitbill.domain.split.SplitType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response bodies. Every monetary amount is a <b>string</b> ("1500000.00"): JSON numbers are
 * parsed as IEEE-754 doubles by many clients (JavaScript in particular), which silently loses precision.
 */
public final class Responses {

    private Responses() {
    }

    public record ParticipantResponse(UUID id, String name) {
        public static ParticipantResponse from(ParticipantView v) {
            return new ParticipantResponse(v.id(), v.name());
        }
    }

    public record GroupResponse(UUID id, String name, String currency, List<ParticipantResponse> participants,
                                Instant createdAt) {
        public static GroupResponse from(GroupView v) {
            return new GroupResponse(v.id(), v.name(), v.currency().getCurrencyCode(),
                    v.participants().stream().map(ParticipantResponse::from).toList(), v.createdAt());
        }
    }

    public record ShareResponse(UUID participantId, String amount) {
    }

    public record ExpenseResponse(UUID id, UUID groupId, String description, ExpenseCategory category,
                                  String amount, String currency, UUID paidBy, SplitType splitType,
                                  ExpenseStatus status, String voidReason, List<ShareResponse> shares,
                                  Instant createdAt, Instant voidedAt) {
        public static ExpenseResponse from(ExpenseView v) {
            return new ExpenseResponse(v.id(), v.groupId(), v.description(), v.category(),
                    v.amount().toPlainString(), v.amount().currency().getCurrencyCode(), v.paidBy(),
                    v.splitType(), v.status(), v.voidReason(),
                    v.shares().stream().map(s -> new ShareResponse(s.participantId(), s.amount().toPlainString()))
                            .toList(),
                    v.createdAt(), v.voidedAt());
        }
    }

    public record PaymentResponse(UUID id, UUID groupId, UUID fromParticipantId, UUID toParticipantId,
                                  String amount, String currency, String note, Instant createdAt) {
        public static PaymentResponse from(PaymentView v) {
            return new PaymentResponse(v.id(), v.groupId(), v.fromParticipantId(), v.toParticipantId(),
                    v.amount().toPlainString(), v.amount().currency().getCurrencyCode(), v.note(), v.createdAt());
        }
    }

    public record BalanceResponse(UUID participantId, String name, String paid, String share,
                                  String paymentsSent, String paymentsReceived, String net) {
        public static BalanceResponse from(BalanceView v) {
            return new BalanceResponse(v.participantId(), v.name(), v.paid().toPlainString(),
                    v.share().toPlainString(), v.paymentsSent().toPlainString(),
                    v.paymentsReceived().toPlainString(), v.net().toPlainString());
        }
    }

    public record TransferResponse(UUID fromParticipantId, String fromName, UUID toParticipantId, String toName,
                                   String amount) {
        public static TransferResponse from(TransferView v) {
            return new TransferResponse(v.fromParticipantId(), v.fromName(), v.toParticipantId(), v.toName(),
                    v.amount().toPlainString());
        }
    }

    public record CategoryTotalResponse(ExpenseCategory category, String total, int expenseCount) {
        public static CategoryTotalResponse from(CategoryTotalView v) {
            return new CategoryTotalResponse(v.category(), v.total().toPlainString(), v.expenseCount());
        }
    }

    public record ServiceChargeShareResponse(UUID participantId, String name, String amount) {
        public static ServiceChargeShareResponse from(ServiceChargeShareView v) {
            return new ServiceChargeShareResponse(v.participantId(), v.name(), v.amount().toPlainString());
        }
    }

    public record SettlementResponse(UUID groupId,
                                     String groupName,
                                     String currency,
                                     String totalExpenses,
                                     String totalPayments,
                                     int serviceChargePct,
                                     String serviceChargeAmount,
                                     List<ServiceChargeShareResponse> serviceChargeBreakdown,
                                     List<BalanceResponse> balances,
                                     List<TransferResponse> transfers,
                                     int transactionCount,
                                     boolean settled,
                                     List<CategoryTotalResponse> categorySummary,
                                     Instant generatedAt) {
        public static SettlementResponse from(SettlementView v) {
            return new SettlementResponse(
                    v.groupId(),
                    v.groupName(),
                    v.currency().getCurrencyCode(),
                    v.totalExpenses().toPlainString(),
                    v.totalPayments().toPlainString(),
                    v.serviceChargePct(),
                    v.serviceChargeAmount().toPlainString(),
                    v.serviceChargeBreakdown().stream().map(ServiceChargeShareResponse::from).toList(),
                    v.balances().stream().map(BalanceResponse::from).toList(),
                    v.transfers().stream().map(TransferResponse::from).toList(),
                    v.transfers().size(),
                    v.transfers().isEmpty(),
                    v.categorySummary().stream().map(CategoryTotalResponse::from).toList(),
                    v.generatedAt());
        }
    }

    public record AuditEventResponse(UUID id, String entityType, UUID entityId, AuditAction action, String details,
                                     Instant occurredAt) {
        public static AuditEventResponse from(AuditEventView v) {
            return new AuditEventResponse(v.id(), v.entityType(), v.entityId(), v.action(), v.details(),
                    v.occurredAt());
        }
    }
}
