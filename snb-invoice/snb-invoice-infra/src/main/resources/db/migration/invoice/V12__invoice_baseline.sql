-- invoice 上下文基线：抬头 + 申请单 + 订单占用明细 + 发票 PDF（设计稿 ai-relay specs/2026-07-15-invoice-design.md）
-- 审计列照 content.article 模板（patra 审计基座）；id 为应用层雪花，无数据库自增。
-- 金额单位 CNY，numeric(12,2)。

CREATE TABLE invoice.invoice_profile (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    type            TEXT   NOT NULL CHECK (type IN ('COMPANY', 'PERSONAL')),
    title           TEXT   NOT NULL,
    tax_no          TEXT,
    reg_address     TEXT,
    reg_phone       TEXT,
    bank_name       TEXT,
    bank_account    TEXT,
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
CREATE INDEX idx_invoice_profile_user ON invoice.invoice_profile (user_id);

CREATE TABLE invoice.invoice_request (
    id                   BIGINT PRIMARY KEY,
    request_no           TEXT   NOT NULL UNIQUE,
    user_id              BIGINT NOT NULL,
    amount               NUMERIC(12,2) NOT NULL,
    fee                  NUMERIC(12,2) NOT NULL,
    status               TEXT   NOT NULL CHECK (status IN ('PENDING', 'INVOICING', 'ISSUED', 'REJECTED', 'CANCELLED')),
    profile_type         TEXT   NOT NULL,
    profile_title        TEXT   NOT NULL,
    profile_tax_no       TEXT,
    profile_reg_address  TEXT,
    profile_reg_phone    TEXT,
    profile_bank_name    TEXT,
    profile_bank_account TEXT,
    remark               TEXT,
    reject_reason        TEXT,
    fee_charged_at       TIMESTAMPTZ,
    issued_at            TIMESTAMPTZ,
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
CREATE INDEX idx_invoice_request_user   ON invoice.invoice_request (user_id, created_at DESC);
CREATE INDEX idx_invoice_request_status ON invoice.invoice_request (status, created_at DESC);
-- 同一用户同时只有一张进行中申请（并发双开的数据库兜底）
CREATE UNIQUE INDEX ux_invoice_request_active_per_user
    ON invoice.invoice_request (user_id) WHERE status IN ('PENDING', 'INVOICING');

CREATE TABLE invoice.invoice_request_order (
    id           BIGINT PRIMARY KEY,
    request_id   BIGINT NOT NULL REFERENCES invoice.invoice_request(id),
    order_id     BIGINT NOT NULL,
    order_no     TEXT   NOT NULL,
    amount       NUMERIC(12,2) NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_invoice_request_order_request ON invoice.invoice_request_order (request_id);
-- 一笔充值订单同时只能被一张有效申请占用（防重复开票的数据库级保证）
CREATE UNIQUE INDEX ux_invoice_order_active ON invoice.invoice_request_order (order_id) WHERE active;

CREATE TABLE invoice.invoice_pdf (
    id         BIGINT PRIMARY KEY,
    request_id BIGINT NOT NULL UNIQUE REFERENCES invoice.invoice_request(id),
    bytes      BYTEA  NOT NULL,
    filename   TEXT   NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256     TEXT   NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT NOT NULL DEFAULT 0
);
