-- ═══════════════════════════════════════════════════════════════════════════
-- V18 开学季·带兄弟来包机(2026-08-13, ai-relay runbook 41)。
-- 审计基座同 V8/V17:BaseJpaEntity 全审计列组,id 一律应用层预分配雪花 BIGINT,
-- 无数据库自增;created_at/updated_at/version 带 DEFAULT 兜底纯 SQL 写入。
--
-- 两类领取合一张表:
--   kind=first_charge  → tier=卡面(50/100/200),新客首充礼三档取最高档、每号一次
--   kind=milestone     → tier=人数档(1/3/6),带人里程碑逐档累计可叠加
-- (user_id, kind, tier) 唯一键 = 并发仲裁真源;grant_status 状态机 pending → success | failed。
--
-- ⚠️ 判重唯一真源=本表,绝不用 user_subscriptions.notes 匹配——疯四 alreadyClaimed
-- 精确匹配被兑换码续期追加 notes 打穿的教训(runbook ai-relay deployment/36)。
-- 同批代码:usecase/school/*、ActivityController /school/* 端点。
-- ═══════════════════════════════════════════════════════════════════════════
CREATE TABLE activity.school_claim (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    kind            TEXT NOT NULL,                    -- first_charge | milestone
    tier            INT NOT NULL,
    group_id        BIGINT NOT NULL,                  -- 发的 sub2api 分组
    grant_status    TEXT NOT NULL DEFAULT 'pending',  -- pending | success | failed
    attempts        INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    record_remarks  JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    created_by_name TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    updated_by_name TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    ip_address      BYTEA,
    CONSTRAINT uq_school_claim_user_kind_tier UNIQUE (user_id, kind, tier)
);
-- 页面 status 按用户捞全量领取态
CREATE INDEX idx_school_claim_user ON activity.school_claim (user_id);
-- 收官对账/补偿排查按状态捞
CREATE INDEX idx_school_claim_status ON activity.school_claim (grant_status);
