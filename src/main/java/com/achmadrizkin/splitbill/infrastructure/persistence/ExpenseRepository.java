package com.achmadrizkin.splitbill.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {

    List<ExpenseEntity> findByGroupIdOrderByCreatedAtAscIdAsc(UUID groupId);

    Optional<ExpenseEntity> findByIdAndGroupId(UUID id, UUID groupId);
}
