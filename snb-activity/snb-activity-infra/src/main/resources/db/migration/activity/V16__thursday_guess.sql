-- 猜桶竞猜(开业活动 spec §3 / 站长 07-29 改制:判定客观零人工裁量)。
-- 猜的是「今晚 22:00 结算时疯四桶出多少份」,封猜后不可改,最接近者胜、并列取最早提交。
--
-- 为什么这是本功能唯一的表:发卡与隐藏款摇号都能从 payment_orders 推出来(零存储),
-- 唯独用户猜的数字是凭空的新输入,推不出来,只能存。
--
-- (session_date, user_id) 唯一键 = 「一人一场一猜」的并发仲裁真源,也让「已猜过就不能改」
-- 变成库层面的事实而不是应用层的君子协定。
-- 结算结果不落表:赢家 = 按 |guess - 实际桶数| 排序、并列取 created_at 最早,随时可重算,
-- 因此天然不可能改判(与隐藏款摇号同一个思路)。
-- 审计列套件照 V5/V7 家族式(BaseJpaEntity 契约)。

CREATE TABLE activity.thursday_guess (
    id              BIGINT PRIMARY KEY,
    session_date    DATE   NOT NULL,
    user_id         BIGINT NOT NULL,
    guess           INT    NOT NULL,
    record_remarks  JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    created_by_name TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    updated_by_name TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    ip_address      BYTEA,
    CONSTRAINT uq_thursday_guess_user_session UNIQUE (session_date, user_id)
);

-- 结算时按场次全取(按提交时刻排,并列取最早);人数统计也走它。
CREATE INDEX idx_thursday_guess_session ON activity.thursday_guess (session_date, created_at);
