package com.achmadrizkin.splitbill.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A settle-up payment that happened outside the system (cash, bank transfer). Immutable. */
@Entity
@Table(name = "payment")
public class PaymentEntity extends BaseEntity {

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "from_id", nullable = false, updatable = false)
    private UUID fromId;

    @Column(name = "to_id", nullable = false, updatable = false)
    private UUID toId;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(length = 200, updatable = false)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(UUID id, UUID groupId, UUID fromId, UUID toId, BigDecimal amount, String note,
                         Instant createdAt) {
        super(id);
        this.groupId = groupId;
        this.fromId = fromId;
        this.toId = toId;
        this.amount = amount;
        this.note = note;
        this.createdAt = createdAt;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getFromId() {
        return fromId;
    }

    public UUID getToId() {
        return toId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
