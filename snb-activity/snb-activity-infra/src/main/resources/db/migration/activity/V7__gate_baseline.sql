-- 金票闸机(spec ai-relay docs/superpowers/specs/2026-07-12-gate-golden-ticket-design.md §4):
-- 码池 + 每日抽签记录。码由站长灌入(runbook 27,真码绝不入 git),发完即止=预算硬封顶;
-- gate_attempt (user_id, attempt_date) 唯一键 = 「每人每日一次」的并发仲裁真源。
-- 两表均带家族审计列套件(BaseJpaEntity 契约,照 V5 raffle 表式)。

CREATE TABLE activity.gate_ticket (
    id              BIGINT PRIMARY KEY,
    amount          NUMERIC(10, 2) NOT NULL,
    code            TEXT NOT NULL,
    claimed_by      BIGINT,
    claimed_at      TIMESTAMPTZ,
    record_remarks  JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    created_by_name TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    updated_by_name TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    ip_address      BYTEA
);
CREATE INDEX idx_gate_ticket_available ON activity.gate_ticket (id) WHERE claimed_by IS NULL;
CREATE INDEX idx_gate_ticket_claimed   ON activity.gate_ticket (claimed_by) WHERE claimed_by IS NOT NULL;

CREATE TABLE activity.gate_attempt (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    attempt_date    DATE   NOT NULL,
    won             BOOLEAN NOT NULL,
    ticket_id       BIGINT REFERENCES activity.gate_ticket (id),
    record_remarks  JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    created_by_name TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    updated_by_name TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    ip_address      BYTEA,
    CONSTRAINT uq_gate_attempt_user_day UNIQUE (user_id, attempt_date)
);
