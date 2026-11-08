package com.achmadrizkin.splitbill.infrastructure.persistence;

import com.achmadrizkin.splitbill.domain.audit.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "audit_event")
public class AuditEventEntity extends BaseEntity {

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(length = 1000)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(UUID id, UUID groupId, String entityType, UUID entityId, AuditAction action,
                            String details, Instant occurredAt) {
        super(id);
        this.groupId = groupId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.details = details;
        this.occurredAt = occurredAt;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
