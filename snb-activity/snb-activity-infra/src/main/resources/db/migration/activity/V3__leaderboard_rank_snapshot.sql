-- 排行榜名次每日快照:升降箭头 delta 的对比基准(spec §10)。
-- 复合自然主键、无雪花 id——本表只被纯 SQL upsert 写入,自然键天然幂等,
-- 刻意绕开「纯 SQL 写入须应用层显式预分配雪花 id」的仓库坑。
CREATE TABLE activity.leaderboard_rank_snapshot (
    snapshot_date DATE   NOT NULL,
    period        TEXT   NOT NULL,  -- day|week|month|all
    metric        TEXT   NOT NULL,  -- tokens|amount
    user_id       BIGINT NOT NULL,
    rank          INT    NOT NULL,
    PRIMARY KEY (snapshot_date, period, metric, user_id)
);
