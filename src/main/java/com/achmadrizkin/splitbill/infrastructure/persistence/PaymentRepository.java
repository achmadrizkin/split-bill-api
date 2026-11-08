package com.achmadrizkin.splitbill.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    List<PaymentEntity> findByGroupIdOrderByCreatedAtAscIdAsc(UUID groupId);

    Optional<PaymentEntity> findByIdAndGroupId(UUID id, UUID groupId);
}
