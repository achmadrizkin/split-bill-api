package com.achmadrizkin.splitbill.infrastructure.persistence;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Aggregate root. Its {@code version} serializes concurrent writes to the same group. */
@Entity
@Table(name = "bill_group")
public class BillGroupEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ParticipantEntity> participants = new ArrayList<>();

    protected BillGroupEntity() {
    }

    public BillGroupEntity(UUID id, String name, Currency currency, Instant now) {
        super(id);
        this.name = name;
        this.currency = currency.getCurrencyCode();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public ParticipantEntity addParticipant(UUID participantId, String participantName, Instant now) {
        String normalized = ParticipantEntity.normalize(participantName);
        boolean exists = participants.stream().anyMatch(p -> p.getNormalizedName().equals(normalized));
        if (exists) {
            throw new DomainException(ErrorCode.DUPLICATE_PARTICIPANT,
                    "Participant '%s' already exists in this group".formatted(participantName.trim()));
        }
        ParticipantEntity participant =
                new ParticipantEntity(participantId, this, participantName.trim(), participants.size(), now);
        participants.add(participant);
        touch(now);
        return participant;
    }

    public void requireParticipant(UUID participantId, String role) {
        if (participantId == null || participants.stream().noneMatch(p -> p.getId().equals(participantId))) {
            throw new DomainException(ErrorCode.PARTICIPANT_NOT_IN_GROUP,
                    "The %s (%s) is not a participant of this group".formatted(role, participantId));
        }
    }

    public List<UUID> participantIds() {
        return participants.stream().map(ParticipantEntity::getId).toList();
    }

    public Map<UUID, String> participantNames() {
        Map<UUID, String> names = new LinkedHashMap<>();
        participants.forEach(p -> names.put(p.getId(), p.getName()));
        return names;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    public Currency currencyUnit() {
        return Currency.getInstance(currency);
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ParticipantEntity> getParticipants() {
        return Collections.unmodifiableList(participants);
    }
}
