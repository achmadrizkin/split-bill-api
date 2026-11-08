package com.achmadrizkin.splitbill.infrastructure.persistence;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.expense.ExpenseCategory;
import com.achmadrizkin.splitbill.domain.expense.ExpenseStatus;
import com.achmadrizkin.splitbill.domain.split.SplitType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Immutable once recorded, except for being voided (never deleted). */
@Entity
@Table(name = "expense")
public class ExpenseEntity extends BaseEntity {

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(nullable = false, length = 200, updatable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "payer_id", nullable = false, updatable = false)
    private UUID payerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20, updatable = false)
    private SplitType splitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status;

    @Column(name = "void_reason", length = 200)
    private String voidReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @ElementCollection
    @CollectionTable(name = "expense_share", joinColumns = @JoinColumn(name = "expense_id"))
    @OrderColumn(name = "sort_order")
    private List<ExpenseShareEmbeddable> shares = new ArrayList<>();

    protected ExpenseEntity() {
    }

    public ExpenseEntity(UUID id, UUID groupId, String description, ExpenseCategory category, BigDecimal amount,
                         UUID payerId, SplitType splitType, List<ExpenseShareEmbeddable> shares, Instant createdAt) {
        super(id);
        this.groupId = groupId;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.payerId = payerId;
        this.splitType = splitType;
        this.status = ExpenseStatus.ACTIVE;
        this.shares = new ArrayList<>(shares);
        this.createdAt = createdAt;
    }

    public void voidExpense(String reason, Instant now) {
        if (status == ExpenseStatus.VOIDED) {
            throw new DomainException(ErrorCode.EXPENSE_ALREADY_VOIDED, "Expense " + getId() + " is already voided");
        }
        this.status = ExpenseStatus.VOIDED;
        this.voidReason = reason;
        this.voidedAt = now;
    }

    public boolean isActive() {
        return status == ExpenseStatus.ACTIVE;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getDescription() {
        return description;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public UUID getPayerId() {
        return payerId;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public List<ExpenseShareEmbeddable> getShares() {
        return Collections.unmodifiableList(shares);
    }
}
