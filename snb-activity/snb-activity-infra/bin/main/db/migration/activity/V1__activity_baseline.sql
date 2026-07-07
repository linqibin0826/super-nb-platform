-- activity 上下文基线 schema(最终形态)。对齐旧 act_* 结构,去前缀、进 activity schema。
-- 审计基座(patra):id 一律应用层预分配雪花 BIGINT,无数据库自增;
-- created_at/updated_at/version 带 DEFAULT 兜底纯 SQL 写入(数据迁移/运维脚本),JPA 路径由审计自动填充。
CREATE SCHEMA IF NOT EXISTS activity;

-- 活动(聚合根,BaseJpaEntity 全审计列组)
CREATE TABLE activity.campaign (
    id                 BIGINT PRIMARY KEY,
    name               TEXT NOT NULL,
    starts_at          TIMESTAMPTZ NOT NULL,
    ends_at            TIMESTAMPTZ NOT NULL,             -- 排他上界
    status             TEXT NOT NULL DEFAULT 'active',   -- active | ended
    consolation_amount NUMERIC(20,2) NOT NULL DEFAULT 5,
    record_remarks     JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         BIGINT,
    created_by_name    TEXT,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by         BIGINT,
    updated_by_name    TEXT,
    version            BIGINT NOT NULL DEFAULT 0,
    ip_address         BYTEA
);

-- 奖槽(子实体,ChildJpaEntity:领奖是独立更新语义)
CREATE TABLE activity.prize_slot (
    id           BIGINT PRIMARY KEY,
    campaign_id  BIGINT NOT NULL REFERENCES activity.campaign(id),
    amount       NUMERIC(20,2) NOT NULL,
    redeem_code  TEXT NOT NULL,                     -- 预生成的 sub2api balance 兑换码
    status       TEXT NOT NULL DEFAULT 'available', -- available | claimed
    claimed_by   BIGINT,                            -- sub2api user id
    claimed_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_slot_avail ON activity.prize_slot (campaign_id, status);

-- 抽奖记录(聚合根,BaseJpaEntity;created_by 由审计自动填=抽奖用户)
CREATE TABLE activity.draw (
    id             BIGINT PRIMARY KEY,
    campaign_id    BIGINT NOT NULL REFERENCES activity.campaign(id),
    user_id        BIGINT NOT NULL,                       -- sub2api user id
    slot_id        BIGINT REFERENCES activity.prize_slot(id), -- 安慰奖为 NULL
    amount         NUMERIC(20,2) NOT NULL,
    redeem_code    TEXT,                                  -- 安慰奖 NULL
    is_consolation BOOLEAN NOT NULL DEFAULT false,
    record_remarks     JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         BIGINT,
    created_by_name    TEXT,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by         BIGINT,
    updated_by_name    TEXT,
    version            BIGINT NOT NULL DEFAULT 0,
    ip_address         BYTEA
);
CREATE INDEX idx_draw_user ON activity.draw (campaign_id, user_id);
