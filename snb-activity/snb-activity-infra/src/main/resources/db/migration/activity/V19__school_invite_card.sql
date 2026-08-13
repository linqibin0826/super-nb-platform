-- ═══════════════════════════════════════════════════════════════════════════
-- V19 包机·邀请卡养成 + 重置银行(2026-08-13 玩法 v2,spec §4-v2,ai-relay runbook 41)。
-- 审计基座同 V18:BaseJpaEntity 全审计列组,id 应用层预分配雪花 BIGINT。
--
-- 一人一张邀请卡(user_id 唯一):tier=已领档位(1=Go/2=Plus/3=ProLite/4=Pro),
-- subscription_id=sub2api 订阅 id(重置额度要用;升档换组后更新)。
-- 重置银行采用推导制:获得侧 earned(n)=n−|{节点≤n}| 永远从合格人数现推(防事件丢失),
-- 只有 resets_used 落库;可用 = earned − used。
--
-- V18 school_claim 的 kind=milestone 行随 1/3/6 阶梯制退役不再写入(留档无害),
-- kind=first_charge 首充线继续使用。
-- ═══════════════════════════════════════════════════════════════════════════
CREATE TABLE activity.school_card (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    tier            INT NOT NULL,                     -- 1=Go 2=Plus 3=ProLite 4=Pro
    subscription_id BIGINT NOT NULL,                  -- sub2api user_subscriptions.id
    resets_used     INT NOT NULL DEFAULT 0,
    record_remarks  JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    created_by_name TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    updated_by_name TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    ip_address      BYTEA,
    CONSTRAINT uq_school_card_user UNIQUE (user_id)
);
