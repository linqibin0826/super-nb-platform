-- 签到核心基线(V8)。审计基座同 V1/V5/V7:BaseJpaEntity 全审计列组,
-- id 一律应用层预分配雪花 BIGINT,无数据库自增;created_at/updated_at/version 带 DEFAULT
-- 兜底纯 SQL 写入,JPA 路径由审计自动填充。checked_in_at 精确时间戳按深化稿决策⑥现在即入表
-- (无论后续是否裁彩蛋类成就,该列都不可延后——现在不加以后就是表改造)。

-- 签到记录:(user_id, checkin_date) 唯一键=并发仲裁真源,照抄 gate_attempt 模板。
CREATE TABLE activity.checkin_record (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    checkin_date    DATE NOT NULL,
    checked_in_at   TIMESTAMPTZ NOT NULL,
    record_remarks  JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    created_by_name TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    updated_by_name TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    ip_address      BYTEA,
    CONSTRAINT uq_checkin_record_user_day UNIQUE (user_id, checkin_date)
);
-- 月度格子/streak/累计天数查询共用此索引(按用户倒序扫最近日期)
CREATE INDEX idx_checkin_record_user_date ON activity.checkin_record (user_id, checkin_date DESC);

-- 补给资格发放台账:每人每自然月限领 1 档,兼作 bulk-assign 幂等占位与失败重试状态机。
-- status: pending(已占位待发放) | success | failed | deferred(预算硬顶打满显式排队)。
CREATE TABLE activity.checkin_reward_grant (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    grant_month     DATE NOT NULL,               -- 当月第一天,如 2026-07-01
    tier            TEXT NOT NULL,                -- A | B | C
    group_id        BIGINT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'pending',
    attempts        INT NOT NULL DEFAULT 0,
    notes           TEXT NOT NULL,                -- 固定模板文案(不含时间戳),防误触发 409 冲突
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
    CONSTRAINT uq_checkin_reward_grant_user_month UNIQUE (user_id, grant_month)
);
CREATE INDEX idx_checkin_reward_grant_status ON activity.checkin_reward_grant (status);

-- 每日使用量快照:仿 RankSnapshotJob 惯例,自然键无雪花 id,纯 SQL upsert 天然幂等。
-- 防 usage_logs 未来被清理致「有效签到」口径与累计类成就失真。
CREATE TABLE activity.checkin_usage_daily_snapshot (
    user_id       BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    usage_count   INT NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, snapshot_date)
);

-- 批处理水位线:按 job_name 多开行,本表被 Plan B 的多个 metric 生产者与判定引擎共用
-- (仿 qq-campaign 水位线先例)。自然键无雪花 id。
CREATE TABLE activity.checkin_scan_watermark (
    job_name   TEXT PRIMARY KEY,
    watermark  TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
