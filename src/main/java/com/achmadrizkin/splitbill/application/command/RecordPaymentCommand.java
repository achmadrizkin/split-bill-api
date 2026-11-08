package com.achmadrizkin.splitbill.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordPaymentCommand(UUID fromParticipantId, UUID toParticipantId, BigDecimal amount, String note) {
}
