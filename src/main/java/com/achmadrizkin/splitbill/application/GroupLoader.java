package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Single place that turns "group not found" into a domain error. */
@Component
class GroupLoader {

    private final BillGroupRepository groups;

    GroupLoader(BillGroupRepository groups) {
        this.groups = groups;
    }

    BillGroupEntity load(UUID groupId) {
        return groups.findById(groupId).orElseThrow(() -> notFound(groupId));
    }

    /** Load for a write; see {@link BillGroupRepository#findForUpdate}. */
    BillGroupEntity loadForUpdate(UUID groupId) {
        return groups.findForUpdate(groupId).orElseThrow(() -> notFound(groupId));
    }

    private static DomainException notFound(UUID groupId) {
        return new DomainException(ErrorCode.GROUP_NOT_FOUND, "Group " + groupId + " does not exist");
    }
}
