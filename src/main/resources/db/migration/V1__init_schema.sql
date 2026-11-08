-- Money columns use NUMERIC(19,4): exact decimal, headroom for any ISO 4217 minor-unit scale.

CREATE TABLE bill_group (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100)             NOT NULL,
    currency    VARCHAR(3)               NOT NULL,
    version     BIGINT                   NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE participant (
    id               UUID PRIMARY KEY,
    group_id         UUID                     NOT NULL REFERENCES bill_group (id),
    name             VARCHAR(50)              NOT NULL,
    normalized_name  VARCHAR(50)              NOT NULL,
    sort_order       INT                      NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_participant_name UNIQUE (group_id, normalized_name)
);

CREATE TABLE expense (
    id           UUID PRIMARY KEY,
    group_id     UUID                     NOT NULL REFERENCES bill_group (id),
    description  VARCHAR(200)             NOT NULL,
    category     VARCHAR(30)              NOT NULL,
    amount       NUMERIC(19, 4)           NOT NULL CHECK (amount > 0),
    payer_id     UUID                     NOT NULL REFERENCES participant (id),
    split_type   VARCHAR(20)              NOT NULL,
    status       VARCHAR(20)              NOT NULL,
    void_reason  VARCHAR(200),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    voided_at    TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_expense_group ON expense (group_id);

-- Materialized allocation: the exact cents each participant owes are fixed when the expense is recorded.
CREATE TABLE expense_share (
    expense_id      UUID           NOT NULL REFERENCES expense (id),
    sort_order      INT            NOT NULL,
    participant_id  UUID           NOT NULL REFERENCES participant (id),
    amount          NUMERIC(19, 4) NOT NULL CHECK (amount >= 0),
    PRIMARY KEY (expense_id, sort_order)
);

CREATE TABLE payment (
    id           UUID PRIMARY KEY,
    group_id     UUID                     NOT NULL REFERENCES bill_group (id),
    from_id      UUID                     NOT NULL REFERENCES participant (id),
    to_id        UUID                     NOT NULL REFERENCES participant (id),
    amount       NUMERIC(19, 4)           NOT NULL CHECK (amount > 0),
    note         VARCHAR(200),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_payment_parties CHECK (from_id <> to_id)
);
CREATE INDEX idx_payment_group ON payment (group_id);

-- Append-only audit trail.
CREATE TABLE audit_event (
    id           UUID PRIMARY KEY,
    group_id     UUID                     NOT NULL,
    entity_type  VARCHAR(30)              NOT NULL,
    entity_id    UUID                     NOT NULL,
    action       VARCHAR(30)              NOT NULL,
    details      VARCHAR(1000),
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_audit_group ON audit_event (group_id, occurred_at);

CREATE TABLE idempotency_record (
    id               UUID PRIMARY KEY,
    scope            VARCHAR(100)             NOT NULL,
    idempotency_key  VARCHAR(100)             NOT NULL,
    request_hash     VARCHAR(64)              NOT NULL,
    resource_id      UUID                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_idempotency UNIQUE (scope, idempotency_key)
);
