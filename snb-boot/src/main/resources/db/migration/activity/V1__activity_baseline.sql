-- activity 上下文基线 schema(最终形态)。对齐旧 act_* 结构,去前缀、进 activity schema。
CREATE SCHEMA IF NOT EXISTS activity;

CREATE TABLE activity.campaign (
    id                 BIGSERIAL PRIMARY KEY,
    name               TEXT NOT NULL,
    starts_at          TIMESTAMPTZ NOT NULL,
    ends_at            TIMESTAMPTZ NOT NULL,             -- 排他上界
    status             TEXT NOT NULL DEFAULT 'active',   -- active | ended
    consolation_amount NUMERIC(20,2) NOT NULL DEFAULT 5,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE activity.prize_slot (
    id           BIGSERIAL PRIMARY KEY,
    campaign_id  BIGINT NOT NULL REFERENCES activity.campaign(id),
    amount       NUMERIC(20,2) NOT NULL,
    redeem_code  TEXT NOT NULL,                     -- 预生成的 sub2api balance 兑换码
    status       TEXT NOT NULL DEFAULT 'available', -- available | claimed
    claimed_by   BIGINT,                            -- sub2api user id
    claimed_at   TIMESTAMPTZ
);
CREATE INDEX idx_slot_avail ON activity.prize_slot (campaign_id, status);

CREATE TABLE activity.draw (
    id             BIGSERIAL PRIMARY KEY,
    campaign_id    BIGINT NOT NULL REFERENCES activity.campaign(id),
    user_id        BIGINT NOT NULL,                       -- sub2api user id
    slot_id        BIGINT REFERENCES activity.prize_slot(id), -- 安慰奖为 NULL
    amount         NUMERIC(20,2) NOT NULL,
    redeem_code    TEXT,                                  -- 安慰奖 NULL
    is_consolation BOOLEAN NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_draw_user ON activity.draw (campaign_id, user_id);
