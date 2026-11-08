package com.achmadrizkin.splitbill.api.controller;

final class ApiHeaders {

    /** Client-generated unique key (e.g. a UUID) that makes a POST safe to retry. */
    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    /** "true" when the response is a replay of an earlier request with the same key. */
    static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private ApiHeaders() {
    }
}
