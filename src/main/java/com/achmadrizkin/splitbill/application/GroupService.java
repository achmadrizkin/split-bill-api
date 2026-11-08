package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.application.command.CreateGroupCommand;
import com.achmadrizkin.splitbill.application.view.Views.GroupView;
import com.achmadrizkin.splitbill.application.view.Views.ParticipantView;
import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.audit.AuditAction;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupRepository;
import com.achmadrizkin.splitbill.infrastructure.persistence.ParticipantEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

@Service
public class GroupService {

    static final String DEFAULT_CURRENCY = "IDR";

    private final BillGroupRepository groups;
    private final GroupLoader groupLoader;
    private final AuditService audit;
    private final Clock clock;

    GroupService(BillGroupRepository groups, GroupLoader groupLoader, AuditService audit, Clock clock) {
        this.groups = groups;
        this.groupLoader = groupLoader;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public GroupView create(CreateGroupCommand command) {
        Currency currency = resolveCurrency(command.currency());
        Instant now = clock.instant();

        BillGroupEntity group = new BillGroupEntity(UUID.randomUUID(), command.name().trim(), currency, now);
        command.participantNames().forEach(name -> group.addParticipant(UUID.randomUUID(), name, now));
        groups.save(group);

        audit.record(group.getId(), "GROUP", group.getId(), AuditAction.GROUP_CREATED,
                "name=%s currency=%s participants=%s".formatted(group.getName(), currency.getCurrencyCode(),
                        group.participantNames().values()));
        return ViewMapper.group(group);
    }

    @Transactional(readOnly = true)
    public GroupView get(UUID groupId) {
        return ViewMapper.group(groupLoader.load(groupId));
    }

    @Transactional
    public ParticipantView addParticipant(UUID groupId, String name) {
        BillGroupEntity group = groupLoader.loadForUpdate(groupId);
        ParticipantEntity participant = group.addParticipant(UUID.randomUUID(), name, clock.instant());

        audit.record(groupId, "PARTICIPANT", participant.getId(), AuditAction.PARTICIPANT_ADDED,
                "name=" + participant.getName());
        return ViewMapper.participant(participant);
    }

    private static Currency resolveCurrency(String code) {
        String normalized = code == null || code.isBlank() ? DEFAULT_CURRENCY : code.trim().toUpperCase(Locale.ROOT);
        try {
            Currency currency = Currency.getInstance(normalized);
            Money.scaleOf(currency); // rejects pseudo-currencies like XAU
            return currency;
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.INVALID_CURRENCY, "Unsupported currency: " + code);
        }
    }
}
