-- NB 账本(V11):NB 值唯一真源 = SUM(points)。EARN(打卡/成就解锁)正数,SPEND(未来商城)负数;
-- 已入账行永不删改。账本行 id 复用来源行雪花 id(unlock 行/checkin_record 行)——补铸零生成器
-- 依赖,审计时账本行与来源行一眼对上;幂等靠 (user_id, source_type, source_ref) 唯一约束。
-- 设计稿:ai-relay docs/superpowers/specs/2026-07-15-checkin-nb-ledger-design.md
CREATE TABLE activity.nb_ledger (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    entry_type      TEXT   NOT NULL,
    source_type     TEXT   NOT NULL,
    source_ref      TEXT   NOT NULL,
    points          INT    NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    created_by_name TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    updated_by_name TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_nb_ledger_source UNIQUE (user_id, source_type, source_ref),
    CONSTRAINT ck_nb_ledger_entry_type CHECK (entry_type IN ('EARN', 'SPEND')),
    CONSTRAINT ck_nb_ledger_sign CHECK (
        (entry_type = 'EARN'  AND points > 0) OR
        (entry_type = 'SPEND' AND points < 0))
);

CREATE INDEX idx_nb_ledger_user ON activity.nb_ledger (user_id);

-- 补铸①:历史成就解锁 → 账本。过滤 revoked(对齐 myUnlocks 读口径)与 points=0(CHECK 要求 EARN>0)。
INSERT INTO activity.nb_ledger (id, user_id, entry_type, source_type, source_ref, points, occurred_at)
SELECT u.id, u.user_id, 'EARN', 'achievement_unlock', u.achievement_code, u.points_at_unlock, u.unlocked_at
FROM activity.achievement_unlock u
WHERE u.revoked_at IS NULL AND u.points_at_unlock > 0
ON CONFLICT (user_id, source_type, source_ref) DO NOTHING;

-- 补铸②:历史打卡 → 账本。单价按 2026-07-15 拍板值 3 固化(调价不追溯);覆盖「签到先上线、
-- 本功能后上」时间差,幂等重跑零新增。
INSERT INTO activity.nb_ledger (id, user_id, entry_type, source_type, source_ref, points, occurred_at)
SELECT c.id, c.user_id, 'EARN', 'checkin_daily', to_char(c.checkin_date, 'YYYY-MM-DD'), 3, c.checked_in_at
FROM activity.checkin_record c
ON CONFLICT (user_id, source_type, source_ref) DO NOTHING;
