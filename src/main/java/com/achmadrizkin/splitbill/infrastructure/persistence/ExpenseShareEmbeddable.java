package com.achmadrizkin.splitbill.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class ExpenseShareEmbeddable {

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    protected ExpenseShareEmbeddable() {
    }

    public ExpenseShareEmbeddable(UUID participantId, BigDecimal amount) {
        this.participantId = participantId;
        this.amount = amount;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
