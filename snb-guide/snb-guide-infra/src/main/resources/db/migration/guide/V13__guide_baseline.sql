-- guide 上下文基线:通用「用户引导已读」标记表。
-- 抽象轴 = guide_key(站点.场景.版本,如 invoice.intro.v1),以后任何站的引导都写这一张表。
-- created_at 即已读时刻;UNIQUE(user_id, guide_key) 保幂等,重复 ack 由应用层吞唯一冲突。

CREATE TABLE guide.guide_ack (
    id         BIGINT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    guide_key  TEXT   NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id, guide_key)
);
