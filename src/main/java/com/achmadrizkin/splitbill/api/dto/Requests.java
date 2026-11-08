package com.achmadrizkin.splitbill.api.dto;

import com.achmadrizkin.splitbill.application.command.AddExpenseCommand;
import com.achmadrizkin.splitbill.application.command.CreateGroupCommand;
import com.achmadrizkin.splitbill.application.command.RecordPaymentCommand;
import com.achmadrizkin.splitbill.domain.expense.ExpenseCategory;
import com.achmadrizkin.splitbill.domain.split.SplitPortion;
import com.achmadrizkin.splitbill.domain.split.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request bodies (JSON is snake_case). Amounts are accepted as JSON strings ("1500000.00") or numbers;
 * either way they are parsed straight into {@link BigDecimal}, never through a double.
 * Bean Validation checks shape; business rules (sums, membership, currency scale) live in the domain.
 */
public final class Requests {

    private Requests() {
    }

    public record CreateGroupRequest(
            @NotBlank @Size(max = 100) String name,
            @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter ISO 4217 code") String currency,
            @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 50) String> participants) {

        public CreateGroupCommand toCommand() {
            return new CreateGroupCommand(name, currency, participants);
        }
    }

    public record AddParticipantRequest(@NotBlank @Size(max = 50) String name) {
    }

    public record SplitEntryRequest(
            @NotNull UUID participantId,
            @Digits(integer = 15, fraction = 4) BigDecimal value) {
    }

    public record AddExpenseRequest(
            @NotBlank @Size(max = 200) String description,
            ExpenseCategory category,
            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
            @NotNull UUID paidBy,
            SplitType splitType,
            @Size(max = 50) List<@Valid @NotNull SplitEntryRequest> splits) {

        public AddExpenseCommand toCommand() {
            List<SplitPortion> portions = splits == null ? List.of()
                    : splits.stream().map(s -> new SplitPortion(s.participantId(), s.value())).toList();
            return new AddExpenseCommand(description, category, amount, paidBy, splitType, portions);
        }
    }

    public record VoidExpenseRequest(@NotBlank @Size(max = 200) String reason) {
    }

    public record RecordPaymentRequest(
            @NotNull UUID fromParticipantId,
            @NotNull UUID toParticipantId,
            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
            @Size(max = 200) String note) {

        public RecordPaymentCommand toCommand() {
            return new RecordPaymentCommand(fromParticipantId, toParticipantId, amount, note);
        }
    }
}
