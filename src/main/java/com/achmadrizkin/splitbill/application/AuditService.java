package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.application.view.Views.AuditEventView;
import com.achmadrizkin.splitbill.domain.audit.AuditAction;
import com.achmadrizkin.splitbill.infrastructure.persistence.AuditEventEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Append-only audit trail, written in the same transaction as the change it describes. */
@Service
public class AuditService {

    private static final int MAX_DETAILS_LENGTH = 1000;

    private final AuditEventRepository events;
    private final GroupLoader groupLoader;
    private final Clock clock;

    AuditService(AuditEventRepository events, GroupLoader groupLoader, Clock clock) {
        this.events = events;
        this.groupLoader = groupLoader;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID groupId, String entityType, UUID entityId, AuditAction action, String details) {
        String safeDetails = details.length() > MAX_DETAILS_LENGTH ? details.substring(0, MAX_DETAILS_LENGTH) : details;
        events.save(new AuditEventEntity(UUID.randomUUID(), groupId, entityType, entityId, action, safeDetails,
                clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<AuditEventView> list(UUID groupId) {
        groupLoader.load(groupId);
        return events.findByGroupIdOrderByOccurredAtAscIdAsc(groupId).stream()
                .map(e -> new AuditEventView(e.getId(), e.getEntityType(), e.getEntityId(), e.getAction(),
                        e.getDetails(), e.getOccurredAt()))
                .toList();
    }
}
