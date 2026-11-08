package com.achmadrizkin.splitbill.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Remembers which resource a given (scope, Idempotency-Key) produced, plus a hash of the request. */
@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecordEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String scope;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecordEntity() {
    }

    public IdempotencyRecordEntity(UUID id, String scope, String idempotencyKey, String requestHash,
                                   UUID resourceId, Instant createdAt) {
        super(id);
        this.scope = scope;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.resourceId = resourceId;
        this.createdAt = createdAt;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public UUID getResourceId() {
        return resourceId;
    }
}
