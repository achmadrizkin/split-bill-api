package com.achmadrizkin.splitbill.domain;

/**
 * Stable, machine-readable error codes. Clients should branch on these, never on messages.
 * The HTTP status mapping lives in the API layer so the domain stays transport-agnostic.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    INVALID_CURRENCY,
    INVALID_AMOUNT,
    AMOUNT_SCALE_EXCEEDED,
    CURRENCY_MISMATCH,
    INVALID_SPLIT,
    SPLIT_SUM_MISMATCH,
    DUPLICATE_PARTICIPANT,
    PARTICIPANT_NOT_IN_GROUP,
    INVALID_PAYMENT,
    GROUP_NOT_FOUND,
    EXPENSE_NOT_FOUND,
    PAYMENT_NOT_FOUND,
    EXPENSE_ALREADY_VOIDED,
    IDEMPOTENCY_KEY_REUSED
}
