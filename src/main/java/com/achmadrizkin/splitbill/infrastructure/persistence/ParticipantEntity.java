package com.achmadrizkin.splitbill.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "participant")
public class ParticipantEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private BillGroupEntity group;

    @Column(nullable = false, length = 50)
    private String name;

    /** Lower-cased name backing the case-insensitive unique constraint ("Andi" == "andi"). */
    @Column(name = "normalized_name", nullable = false, length = 50)
    private String normalizedName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ParticipantEntity() {
    }

    ParticipantEntity(UUID id, BillGroupEntity group, String name, int sortOrder, Instant createdAt) {
        super(id);
        this.group = group;
        this.name = name;
        this.normalizedName = normalize(name);
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    String getNormalizedName() {
        return normalizedName;
    }
}
