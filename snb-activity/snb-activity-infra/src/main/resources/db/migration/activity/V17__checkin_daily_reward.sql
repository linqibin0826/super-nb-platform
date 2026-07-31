-- 每日返网费台账(V17)。审计基座同 V8:BaseJpaEntity 全审计列组,id 一律应用层预分配雪花
-- BIGINT,无数据库自增;created_at/updated_at/version 带 DEFAULT 兜底纯 SQL 写入。
--
-- (user_id, checkin_date) 唯一键 = 幂等真源。⚠️ 上游 admin API 自带的幂等键 TTL 仅 2 小时,
-- 不能当长期幂等依赖——本表才是「一天只发一次」的唯一仲裁。
--
-- 为什么独立成表而不是扩 checkin_record:打卡是本地事务,发余额是外部 HTTP。两者绝不能
-- 同事务——上游一抖就回滚掉用户的打卡是灾难。独立台账 = 幂等真源 + 发放状态机 + 重试 + 对账。
--
-- balance_status: none(未达标/总闸关/预算硬顶打满,不发钱但仍落行) | pending | success | failed
-- 不发钱也落行是刻意的:台账要能回答「这天为什么没发钱」,空行等于查不出原因。
CREATE TABLE activity.checkin_daily_reward (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    checkin_date    DATE NOT NULL,
    streak_day      INT NOT NULL,                  -- 当日连签第几天(N),落库便于对账
    nb_points       INT NOT NULL,                  -- 实发 NB = perDay × N
    balance_cny     NUMERIC(10,2) NOT NULL DEFAULT 0,
    balance_status  TEXT NOT NULL DEFAULT 'none',
    attempts        INT NOT NULL DEFAULT 0,
    notes           TEXT NOT NULL,                 -- 固定模板 checkin-daily-{yyyy-MM-dd},不含时间戳
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
    CONSTRAINT uq_checkin_daily_reward_user_day UNIQUE (user_id, checkin_date)
);
-- 补偿 job 按状态捞重试对象
CREATE INDEX idx_checkin_daily_reward_status ON activity.checkin_daily_reward (balance_status);
-- 预算硬顶按月汇总全站发放额 / 页面「本月已返」按月汇总单人
CREATE INDEX idx_checkin_daily_reward_date ON activity.checkin_daily_reward (checkin_date);
