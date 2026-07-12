-- 报名开奖式抽奖(raffle)基线:campaign(期) / prize(一件一行,payload 机密) / entry(一人一期一条)。
-- 审计基座同 V1:id 一律应用层预分配雪花 BIGINT,无数据库自增;
-- created_at/updated_at/version 带 DEFAULT 兜底纯 SQL 写入(运维建期/灌奖),JPA 路径由审计自动填充。
-- 版本号取 V5:V4 已被 content(hub,main 未发版)占用——Flyway 全局单序列;本分支基于 v0.1.5 无 V4,跳号合法。

CREATE TABLE activity.raffle_campaign (
    id                    BIGINT PRIMARY KEY,
    name                  TEXT NOT NULL,
    entry_open_at         TIMESTAMPTZ NOT NULL,
    entry_close_at        TIMESTAMPTZ NOT NULL,           -- 排他上界;约定 draw_at >= entry_close_at(运维模板负责)
    draw_at               TIMESTAMPTZ NOT NULL,
    gate_type             TEXT NOT NULL,                  -- RECHARGE | SPEND
    gate_amount           NUMERIC(12,2) NOT NULL,
    gate_from             TIMESTAMPTZ NOT NULL,           -- 门槛起算时刻;epoch 起点=全历史
    min_account_age_days  INT,                            -- 可选账龄门槛,NULL=不限
    weight_mode           TEXT NOT NULL DEFAULT 'EQUAL',  -- EQUAL | WEIGHTED(按门槛同指标复核值加权)
    status                TEXT NOT NULL DEFAULT 'active', -- active | drawn | cancelled
    drawn_at              TIMESTAMPTZ,
    entrant_count_at_draw INT,
    disqualified_count    INT,
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
-- 开奖轮询按 (status, draw_at) 走索引,常态几乎零成本
CREATE INDEX idx_raffle_campaign_due ON activity.raffle_campaign (status, draw_at);

-- 奖品一件一行(每张码/每条口令互不相同);payload 机密,任何公开端点不得吐出
CREATE TABLE activity.raffle_prize (
    id             BIGINT PRIMARY KEY,
    campaign_id    BIGINT NOT NULL REFERENCES activity.raffle_campaign(id),
    tier           TEXT NOT NULL,                  -- 档位:S/A/B/C/D 自由文本
    display_name   TEXT NOT NULL,                  -- 官腔展示名(含真实奖品与面值)
    kind           TEXT NOT NULL,                  -- REDEEM_CODE | ALIPAY_CODE
    payload        TEXT NOT NULL,                  -- 机密:sub2api 兑换码或支付宝口令明文
    sort_order     INT NOT NULL DEFAULT 0,         -- 张榜/分配顺序,大奖靠前
    winner_user_id BIGINT,                         -- 开奖写入;NULL=无主(流拍)
    assigned_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_raffle_prize_campaign ON activity.raffle_prize (campaign_id, sort_order);

CREATE TABLE activity.raffle_entry (
    id                  BIGINT PRIMARY KEY,
    campaign_id         BIGINT NOT NULL REFERENCES activity.raffle_campaign(id),
    user_id             BIGINT NOT NULL,            -- sub2api user id
    entry_no            INT NOT NULL,               -- 参会证号,期内连续
    gate_value_at_entry NUMERIC(12,2) NOT NULL,     -- 报名时点门槛指标值(留痕;开奖以复核为准)
    client_ip           TEXT,                       -- 秋后清算日志(X-Forwarded-For 首值)
    user_agent          TEXT,
    record_remarks      JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          BIGINT,
    created_by_name     TEXT,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    updated_by_name     TEXT,
    version             BIGINT NOT NULL DEFAULT 0,
    ip_address          BYTEA,
    CONSTRAINT uq_raffle_entry_user UNIQUE (campaign_id, user_id),
    CONSTRAINT uq_raffle_entry_no   UNIQUE (campaign_id, entry_no)
);
