-- ops 上下文基线:邮箱账号 + 服务开通/订阅(设计稿 ai-relay specs/2026-08-17-snb-ops-account-mvp-design.md)
-- 账号=邮箱单一实体;ChatGPT/Claude 是邮箱开通的服务,每邮箱×服务一行(UNIQUE 兜底)。
-- password_enc 列存 AES-256-GCM 密文(格式 v1:nonce:ct),明文绝不落库。
-- 审计列照 invoice_profile 模板;id 应用层雪花,无自增。

CREATE TABLE ops.account (
    id                    BIGINT PRIMARY KEY,
    email                 TEXT   NOT NULL UNIQUE,
    provider              TEXT,
    password_enc          TEXT,
    recovery_email        TEXT,
    recovery_password_enc TEXT,
    reg_year              TEXT,
    country               TEXT,
    owner                 TEXT,
    status                TEXT   NOT NULL CHECK (status IN ('ACTIVE', 'BANNED', 'UNVERIFIED', 'ABANDONED')),
    source                TEXT,
    notes                 TEXT,
    record_remarks        JSONB,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            BIGINT,
    created_by_name       TEXT,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by            BIGINT,
    updated_by_name       TEXT,
    version               BIGINT NOT NULL DEFAULT 0,
    ip_address            BYTEA
);

CREATE TABLE ops.subscription (
    id                   BIGINT PRIMARY KEY,
    account_id           BIGINT NOT NULL REFERENCES ops.account(id),
    service              TEXT   NOT NULL CHECK (service IN ('CHATGPT', 'CLAUDE')),
    tier                 TEXT   CHECK (tier IN ('FREE', 'PLUS', 'PRO', 'MAX', 'TEAM')),
    region               TEXT,
    card_platform        TEXT,
    card_last4           TEXT,
    register_ip          TEXT,
    current_ip           TEXT,
    registered_at        TIMESTAMPTZ,
    started_at           DATE,
    next_billing_at      DATE,
    price_usd            NUMERIC(8,2),
    status               TEXT   NOT NULL CHECK (status IN ('FREE', 'ACTIVE', 'EXPIRED', 'CANCELED', 'BANNED')),
    sub2api_account_id   BIGINT,
    sub2api_account_name TEXT,
    banned_at            TIMESTAMPTZ,
    banned_while_paid    BOOLEAN,
    refund_status        TEXT   NOT NULL DEFAULT 'NONE'
                         CHECK (refund_status IN ('NONE', 'PENDING', 'APPEALING', 'REFUNDED', 'REJECTED')),
    refund_amount_usd    NUMERIC(8,2),
    appealed_at          DATE,
    refund_resolved_at   DATE,
    refund_follow_up_at  DATE,
    refund_notes         TEXT,
    notes                TEXT,
    record_remarks       JSONB,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           BIGINT,
    created_by_name      TEXT,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           BIGINT,
    updated_by_name      TEXT,
    version              BIGINT NOT NULL DEFAULT 0,
    ip_address           BYTEA
);
-- 一邮箱一服务一行(历史重开走 notes,MVP 不做多轮历史)
CREATE UNIQUE INDEX ux_ops_subscription_account_service ON ops.subscription (account_id, service);
CREATE INDEX idx_ops_subscription_account ON ops.subscription (account_id);
CREATE INDEX idx_ops_subscription_billing ON ops.subscription (next_billing_at);
