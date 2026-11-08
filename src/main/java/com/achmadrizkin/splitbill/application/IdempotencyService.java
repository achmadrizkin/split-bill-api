package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.application.view.Views.IdempotentResult;
import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.infrastructure.persistence.IdempotencyRecordEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Idempotency-Key handling, the same pattern payment APIs (Stripe, Xendit, Midtrans) use so that a
 * client retrying after a timeout never records the same money movement twice.
 *
 * <ul>
 *   <li>First request with a key: run the command, remember (key -> resource id, request hash).</li>
 *   <li>Retry with same key and same payload: return the original resource, do nothing.</li>
 *   <li>Same key, different payload: reject with 409 — almost certainly a client bug.</li>
 * </ul>
 * Runs inside the caller's transaction, so a failed command leaves no idempotency record behind.
 * A concurrent duplicate is stopped by the unique constraint on (scope, key).
 */
@Service
public class IdempotencyService {

    private static final int MAX_KEY_LENGTH = 100;

    private final IdempotencyRecordRepository records;
    private final Clock clock;

    IdempotencyService(IdempotencyRecordRepository records, Clock clock) {
        this.records = records;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> IdempotentResult<T> execute(String scope, String idempotencyKey, Object request,
                                           Supplier<UUID> command, Function<UUID, T> loader) {
        if (idempotencyKey == null) {
            return new IdempotentResult<>(loader.apply(command.get()), false);
        }
        String key = idempotencyKey.trim();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key must be between 1 and " + MAX_KEY_LENGTH + " characters");
        }
        String requestHash = sha256(String.valueOf(request));

        Optional<IdempotencyRecordEntity> existing = records.findByScopeAndIdempotencyKey(scope, key);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new DomainException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                        "Idempotency-Key '" + key + "' was already used with a different request payload");
            }
            return new IdempotentResult<>(loader.apply(existing.get().getResourceId()), true);
        }

        UUID resourceId = command.get();
        records.save(new IdempotencyRecordEntity(UUID.randomUUID(), scope, key, requestHash, resourceId,
                clock.instant()));
        return new IdempotentResult<>(loader.apply(resourceId), false);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
