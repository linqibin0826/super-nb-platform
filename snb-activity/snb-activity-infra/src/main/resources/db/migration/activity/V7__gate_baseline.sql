-- 金票闸机(spec ai-relay docs/superpowers/specs/2026-07-12-gate-golden-ticket-design.md §4):
-- 码池 + 每日抽签记录。码由站长灌入(runbook 27,真码绝不入 git),发完即止=预算硬封顶;
-- gate_attempt (user_id, attempt_date) 唯一键 = 「每人每日一次」的并发仲裁真源。

CREATE TABLE gate_ticket (
    id          BIGINT PRIMARY KEY,
    amount      NUMERIC(10, 2) NOT NULL,
    code        TEXT NOT NULL,
    claimed_by  BIGINT,
    claimed_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_gate_ticket_available ON gate_ticket (id) WHERE claimed_by IS NULL;
CREATE INDEX idx_gate_ticket_claimed   ON gate_ticket (claimed_by) WHERE claimed_by IS NOT NULL;

CREATE TABLE gate_attempt (
    id           BIGINT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    attempt_date DATE   NOT NULL,
    won          BOOLEAN NOT NULL,
    ticket_id    BIGINT REFERENCES gate_ticket (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_gate_attempt_user_day UNIQUE (user_id, attempt_date)
);
