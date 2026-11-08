package com.achmadrizkin.splitbill.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BillGroupRepository extends JpaRepository<BillGroupEntity, UUID> {

    /**
     * Loads the group for a write. OPTIMISTIC_FORCE_INCREMENT bumps the version on commit even if the
     * group row itself is unchanged, so two concurrent writes to the same group (e.g. two payments that
     * each passed the "not more than owed" check) cannot both commit — the loser gets a 409.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select g from BillGroupEntity g where g.id = :id")
    Optional<BillGroupEntity> findForUpdate(@Param("id") UUID id);
}
