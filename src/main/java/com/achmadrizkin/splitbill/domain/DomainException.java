package com.achmadrizkin.splitbill.domain;

import java.util.Objects;

/** A business rule was violated. Carries a stable {@link ErrorCode} for API clients. */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
