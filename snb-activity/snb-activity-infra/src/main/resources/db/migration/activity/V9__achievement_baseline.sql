-- 成就系统架构基线(V9)。依赖 V8(checkin_record/checkin_scan_watermark 等已存在)。
-- 审计基座同 V1/V5/V7/V8。achievement_definition 是 BaseJpaEntity 风格(全审计列组,
-- 运维/发版才会新增行,应用代码不写业务构造器);achievement_unlock 是 ChildJpaEntity 风格
-- (id+created_at/updated_at/version,无 created_by 概念——系统批处理写入)。
-- category/status/predicate_kind 禁用 DB 原生 ENUM/CHECK 约束(深化稿不可逆清单第7条:
-- "加类目=INSERT而非DDL"),一律 VARCHAR,应用层校验清单见 V9 之后的代码任务。

CREATE TABLE activity.achievement_definition (
    id                  BIGINT PRIMARY KEY,
    code                TEXT NOT NULL UNIQUE,
    series_code         TEXT,
    tier_level          INT,
    category            TEXT NOT NULL,
    rarity              TEXT NOT NULL,           -- T1|T2|T3|T4(对外档名"有点NB/挺NB/非常NB/SUPER·NB"由应用层映射)
    nb_points           INT NOT NULL,
    hidden_reveal       BOOLEAN NOT NULL DEFAULT false,
    always_private      BOOLEAN NOT NULL DEFAULT false,
    status              TEXT NOT NULL DEFAULT 'active',  -- draft|active|retired,只能单向前进
    retired_at          TIMESTAMPTZ,
    retire_reason       TEXT,
    predicate_kind      TEXT NOT NULL,           -- metric_threshold|meta_combo|custom_code
    metric_code         TEXT,
    threshold_value     NUMERIC(20,2),
    comparator          TEXT,                     -- gte(默认)|lte(仅排行榜名次越小越好)
    prerequisite        TEXT,                     -- meta_combo 用:类目名或 null(系列引用走通用算法)
    first_batch_no      INT,
    launch_date         DATE,
    sort_order          INT NOT NULL DEFAULT 0,
    name                TEXT NOT NULL,
    condition_text      TEXT NOT NULL,           -- 面向用户的条件文案(深化稿 §3"条件"列逐字照抄,
                                                  -- 不由 metric_code+threshold+comparator 反推——
                                                  -- 反推只能得到"raffle_companion_count ≥ 1"这类技术措辞,
                                                  -- 达不到"报名 raffle 未中奖(仅计已开奖期次)"的成品文案要求)
    flavor_text         TEXT,
    hidden_hint_text    TEXT,
    icon_ref            TEXT,
    record_remarks      JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          BIGINT,
    created_by_name     TEXT,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    updated_by_name     TEXT,
    version             BIGINT NOT NULL DEFAULT 0,
    ip_address          BYTEA
);
CREATE INDEX idx_achievement_definition_status ON activity.achievement_definition (status);
CREATE INDEX idx_achievement_definition_category ON activity.achievement_definition (category);

-- (user_id, achievement_code) 唯一键 = 全系统防重复解锁唯一真源,任何触发路径最终靠此行
-- INSERT 撞键去重(无需 advisory lock——只有事实判断,没有稀缺资源分配)。
CREATE TABLE activity.achievement_unlock (
    id                BIGINT PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    achievement_code  TEXT NOT NULL,
    unlocked_at       TIMESTAMPTZ NOT NULL,
    points_at_unlock  INT NOT NULL,
    unlock_source     TEXT NOT NULL,   -- batch_scan|retroactive_backfill|manual_grant
    seen              BOOLEAN NOT NULL DEFAULT false,
    seen_at           TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    revoke_reason     TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_achievement_unlock_user_code UNIQUE (user_id, achievement_code)
);
CREATE INDEX idx_achievement_unlock_user ON activity.achievement_unlock (user_id);
CREATE INDEX idx_achievement_unlock_unseen ON activity.achievement_unlock (user_id) WHERE seen = false;

-- 指标底座:判定引擎与"原始业务表"之间的缓冲层,自然键无雪花 id(纯 SQL upsert 天然幂等,
-- 仿 leaderboard_rank_snapshot 惯例)。
CREATE TABLE activity.user_metric (
    user_id     BIGINT NOT NULL,
    metric_code TEXT NOT NULL,
    value       NUMERIC(20,2) NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, metric_code)
);
CREATE INDEX idx_user_metric_updated ON activity.user_metric (updated_at);

-- 解锁率派生缓存(日频刷新,前端读此表——稀有度 % 是实时派生值不是入库静态值)。
CREATE TABLE activity.achievement_unlock_stat (
    achievement_code TEXT PRIMARY KEY,
    unlock_count     INT NOT NULL DEFAULT 0,
    unlock_rate      NUMERIC(6,4) NOT NULL DEFAULT 0,
    computed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- MVP 42 条成就目录 seed(深化稿 §3 逐字照抄,id 保留区间 900001-900042)
-- ============================================================

INSERT INTO activity.achievement_definition
    (id, code, series_code, tier_level, category, rarity, nb_points, hidden_reveal, always_private,
     status, predicate_kind, metric_code, threshold_value, comparator, prerequisite,
     first_batch_no, launch_date, sort_order, name, condition_text, flavor_text, hidden_hint_text)
VALUES
-- 入职档案(2)
(900001, 'checkin_first', NULL, NULL, '入职档案', 'T1', 5, false, false, 'active',
 'metric_threshold', 'checkin_total_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 1,
 '开机自检', '首次签到', '每天上班第一件事:证明我还活着。', NULL),
(900002, 'api_first_call', NULL, NULL, '入职档案', 'T1', 5, false, false, 'active',
 'metric_threshold', 'api_call_total_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 2,
 '首次握手', '首次成功调用 API', '握手成功,机房正式收编你。', NULL),

-- 机房作业(5):api_calls 系列 I/II/III + 单发两条
(900003, 'api_calls_1', 'api_calls', 1, '机房作业', 'T1', 5, false, false, 'active',
 'metric_threshold', 'api_call_total_count', 100, 'gte', NULL, 1, DATE '2026-07-13', 10,
 '破壳', '累计调用满 100 次', '第 100 次调用,壳裂了,人还在。', NULL),
(900004, 'api_calls_2', 'api_calls', 2, '机房作业', 'T2', 15, false, false, 'active',
 'metric_threshold', 'api_call_total_count', 1000, 'gte', NULL, 1, DATE '2026-07-13', 11,
 '满负荷', '满 1,000 次', '负载曲线终于有了个像样的形状。', NULL),
(900005, 'api_calls_3', 'api_calls', 3, '机房作业', 'T3', 40, false, false, 'active',
 'metric_threshold', 'api_call_total_count', 10000, 'gte', NULL, 1, DATE '2026-07-13', 12,
 '重型机', '满 10,000 次', '调用量破万,风扇开始有意见。', NULL),
(900006, 'api_daily_peak_1', NULL, NULL, '机房作业', 'T1', 5, false, false, 'active',
 'metric_threshold', 'api_call_daily_peak_max', 100, 'gte', NULL, 1, DATE '2026-07-13', 13,
 '爆机预警', '单日调用≥100 次', '一天干了别人一周的活,机房记下了。', NULL),
(900007, 'cross_surface_user', NULL, NULL, '机房作业', 'T2', 15, false, false, 'active',
 'metric_threshold', 'cross_surface_flag', 1, 'gte', NULL, 1, DATE '2026-07-13', 14,
 '全栈选手', '文本 API 与生图各≥1 次成功', '文字模型和生图模型,你谁都不亏待。', NULL),

-- 补给记录(4):always_private=true(消费分层信息,Phase2 墙转公开前须二次脱敏,深化稿 §7 演化手册待办)
(900008, 'recharge_amount_1', 'recharge_amount', 1, '补给记录', 'T1', 5, false, true, 'active',
 'metric_threshold', 'recharge_total_amount', 0.01, 'gte', NULL, 1, DATE '2026-07-13', 20,
 '破冰', '首次真实充值', '第一笔钱进来,余额表破了处。', NULL),
(900009, 'recharge_amount_2', 'recharge_amount', 2, '补给记录', 'T2', 15, false, true, 'active',
 'metric_threshold', 'recharge_total_amount', 100, 'gte', NULL, 1, DATE '2026-07-13', 21,
 '常客', '累计满 ¥100', '累计满一百,前台已经记住你工位号。', NULL),
(900010, 'recharge_amount_3', 'recharge_amount', 3, '补给记录', 'T3', 40, false, true, 'active',
 'metric_threshold', 'recharge_total_amount', 500, 'gte', NULL, 1, DATE '2026-07-13', 22,
 '补给大户', '累计满 ¥500(顶格)', '累计满五百,茶水间给你留了个杯子。', NULL),
(900011, 'recharge_consistency_1', NULL, NULL, '补给记录', 'T3', 40, false, true, 'active',
 'metric_threshold', 'recharge_consecutive_months', 3, 'gte', NULL, 1, DATE '2026-07-13', 23,
 '细水长流', '连续 3 个月均有真实充值', '三个月,月月都来,比很多正式工靠谱。', NULL),

-- 联动矩阵(9):drawcard/leaderboard/referral 三个系列 + 三个单发
(900012, 'drawcard_1', 'drawcard', 1, '联动矩阵', 'T1', 5, false, false, 'active',
 'metric_threshold', 'drawcard_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 30,
 '开过卡', '开卡≥1 次', '手一抖,卡就开了,爽感与手抖成正比。', NULL),
(900013, 'drawcard_2', 'drawcard', 2, '联动矩阵', 'T2', 15, false, false, 'active',
 'metric_threshold', 'drawcard_count', 10, 'gte', NULL, 1, DATE '2026-07-13', 31,
 '熟练摇臂手', '累计开卡满 10 次', '第十次开卡,手感已经练出来了。', NULL),
(900014, 'raffle_entry_1', NULL, NULL, '联动矩阵', 'T1', 5, false, false, 'active',
 'metric_threshold', 'raffle_entry_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 32,
 '检票入场', '报名 raffle≥1 次', '检票口没拦你,你混进来了。', NULL),
(900015, 'raffle_win_1', NULL, NULL, '联动矩阵', 'T2', 15, false, false, 'active',
 'metric_threshold', 'raffle_win_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 33,
 '天选时刻', '中奖≥1 次', '灯光扫到你工位,机房替你尖叫一声。', NULL),
(900016, 'gate_ticket_1', NULL, NULL, '联动矩阵', 'T3', 40, false, false, 'active',
 'metric_threshold', 'gate_win_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 34,
 '金票中人', '金票中过一次', '五十分之一,你就是那一。', NULL),
(900017, 'leaderboard_1', 'leaderboard', 1, '联动矩阵', 'T2', 15, false, false, 'active',
 'metric_threshold', 'leaderboard_best_rank_ever', 50, 'lte', NULL, 1, DATE '2026-07-13', 35,
 '上榜留名', '任一榜单进 Top50', '前五十名,榜单上有你名字了。', NULL),
(900018, 'leaderboard_2', 'leaderboard', 2, '联动矩阵', 'T3', 40, false, false, 'active',
 'metric_threshold', 'leaderboard_best_rank_ever', 10, 'lte', NULL, 1, DATE '2026-07-13', 36,
 '稳坐钓鱼台', '进 Top10', '前十名,鱼竿一收,位置还在。', NULL),
(900019, 'referral_1', 'referral', 1, '联动矩阵', 'T1', 5, false, false, 'active',
 'metric_threshold', 'referral_valid_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 37,
 '拉新新手', '有效邀请满 1 人', '拉来第一个人,机房人口+1。', NULL),
(900020, 'referral_2', 'referral', 2, '联动矩阵', 'T2', 15, false, false, 'active',
 'metric_threshold', 'referral_valid_count', 3, 'gte', NULL, 1, DATE '2026-07-13', 38,
 '拉新达人', '有效邀请满 3 人', '三个人,够组一支小队了。', NULL),

-- 造像车间(5):image_gen/appreciation 两个系列
(900021, 'image_gen_1', 'image_gen', 1, '造像车间', 'T1', 5, false, false, 'active',
 'metric_threshold', 'gallery_generate_done_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 40,
 '第一张图', '首次生成成功', '第一张图跑出来了,像素都在颤抖。', NULL),
(900022, 'image_gen_2', 'image_gen', 2, '造像车间', 'T2', 15, false, false, 'active',
 'metric_threshold', 'gallery_generate_done_count', 20, 'gte', NULL, 1, DATE '2026-07-13', 41,
 '开工大吉', '累计满 20 张', '二十张图,车间正式投产。', NULL),
(900023, 'image_gen_3', 'image_gen', 3, '造像车间', 'T3', 40, false, false, 'active',
 'metric_threshold', 'gallery_generate_done_count', 100, 'gte', NULL, 1, DATE '2026-07-13', 42,
 '产能拉满', '累计满 100 张', '一百张,产线连轴转都追不上你。', NULL),
(900024, 'appreciation_1', 'appreciation', 1, '造像车间', 'T1', 5, false, false, 'active',
 'metric_threshold', 'gallery_like_fav_count', 20, 'gte', NULL, 1, DATE '2026-07-13', 43,
 '懂鉴赏', '有效点赞+收藏满 20 条', '点赞收藏二十次,审美这事儿你懂。', NULL),
(900025, 'appreciation_2', 'appreciation', 2, '造像车间', 'T2', 15, false, false, 'active',
 'metric_threshold', 'gallery_like_fav_count', 100, 'gte', NULL, 1, DATE '2026-07-13', 44,
 '策展人', '满 100 条', '一百次,你的收藏夹快能办展了。', NULL),

-- 考勤本纪(9):checkin_cum/checkin_full/account_anniv 三个系列 + 两条首发绝版
(900026, 'checkin_cum_1', 'checkin_cum', 1, '考勤本纪', 'T1', 5, false, false, 'active',
 'metric_threshold', 'checkin_total_count', 7, 'gte', NULL, 1, DATE '2026-07-13', 50,
 '打卡新人', '累计签到满 7 天', '攒够七天,考勤簿认识你了。', NULL),
(900027, 'checkin_cum_2', 'checkin_cum', 2, '考勤本纪', 'T2', 15, false, false, 'active',
 'metric_threshold', 'checkin_total_count', 30, 'gte', NULL, 1, DATE '2026-07-13', 51,
 '常驻工位', '满 30 天', '三十天,工位已经算你名下的了。', NULL),
(900028, 'checkin_cum_3', 'checkin_cum', 3, '考勤本纪', 'T3', 40, false, false, 'active',
 'metric_threshold', 'checkin_total_count', 100, 'gte', NULL, 1, DATE '2026-07-13', 52,
 '机房钉子户', '满 100 天', '一百天,拆迁都得绕开你。', NULL),
(900029, 'checkin_full_1', 'checkin_full', 1, '考勤本纪', 'T2', 15, false, false, 'active',
 'metric_threshold', 'checkin_fullmonth_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 53,
 '全勤新兵', '满勤 1 个月(不要求连续)', '一个月一天不落,新兵蛋子干得漂亮。', NULL),
(900030, 'checkin_full_2', 'checkin_full', 2, '考勤本纪', 'T3', 40, false, false, 'active',
 'metric_threshold', 'checkin_fullmonth_count', 3, 'gte', NULL, 1, DATE '2026-07-13', 54,
 '全勤老兵', '满勤 3 个月', '三个月满勤,老兵这称呼不是白叫的。', NULL),
(900031, 'account_anniv_1', 'account_anniv', 1, '考勤本纪', 'T1', 5, false, false, 'active',
 'metric_threshold', 'account_age_days', 100, 'gte', NULL, 1, DATE '2026-07-13', 55,
 '百日报到', '注册满 100 天', '认识满百天,机房开始对你有感情了。', NULL),
(900032, 'account_anniv_2', 'account_anniv', 2, '考勤本纪', 'T2', 15, false, false, 'active',
 'metric_threshold', 'account_age_days', 365, 'gte', NULL, 1, DATE '2026-07-13', 56,
 '周年机长', '满 365 天', '一整年,你比很多工牌寿命都长。', NULL),
(900033, 'exclusive_founding_issue', NULL, NULL, '考勤本纪', 'T4', 130, false, false, 'active',
 'metric_threshold', 'checkin_founding_month_flag', 1, 'gte', NULL, 1, DATE '2026-07-13', 57,
 '创刊号', '上线首月完成≥1 次签到', '考勤簿开簿那个月,你在场——绝版,不再版。', NULL),
(900034, 'exclusive_founding_fullmonth', NULL, NULL, '考勤本纪', 'T4', 150, false, false, 'active',
 'metric_threshold', 'checkin_founding_fullmonth_flag', 1, 'gte', NULL, 1, DATE '2026-07-13', 58,
 '元年全勤', '上线首月满勤', '开簿首月一天没缺,这个印章以后真印不出来了。', NULL),

-- 机密档案(5,全隐藏):hiddenReveal=true,均带 hint;raffle_companion 虽有 I/II 层级但契约
-- 总览示例明确该类目走 items[] 不走 series[],不建系列分组
(900035, 'midnight_courier', NULL, NULL, '机密档案', 'T1', 3, true, false, 'active',
 'metric_threshold', 'checkin_midnight_flag', 1, 'gte', NULL, 1, DATE '2026-07-13', 60,
 '零点信使', '0 点整签到', '零点整准时打卡——闹钟辛苦了。', '表针跳向明天的那一刻,总有人蹲守。'),
(900036, 'late_night_room', NULL, NULL, '机密档案', 'T1', 5, true, false, 'active',
 'metric_threshold', 'api_call_late_night_flag', 1, 'gte', NULL, 1, DATE '2026-07-13', 61,
 '深夜机房', '凌晨 1-5 点有成功调用', '凌晨的机房,只有风扇和你还醒着。', '机房这个点还亮着灯,是谁在。'),
(900037, 'ghost_return', NULL, NULL, '机密档案', 'T1', 5, true, false, 'active',
 'metric_threshold', 'checkin_ghost_return_flag', 1, 'gte', NULL, 1, DATE '2026-07-13', 62,
 '诈尸打卡', '两次签到间隔≥30 天后再签', '消失一个月,考勤簿还是给你留了格。', '考勤簿也认「浪子回头」。'),
(900038, 'raffle_companion_1', NULL, 1, '机密档案', 'T1', 5, true, false, 'active',
 'metric_threshold', 'raffle_companion_count', 1, 'gte', NULL, 1, DATE '2026-07-13', 63,
 '陪跑', '报名 raffle 未中奖(仅计已开奖期次)', '没中奖,但你确实来了,这也算数。', '不是所有到场的人都举得起奖杯。'),
(900039, 'raffle_companion_2', NULL, 2, '机密档案', 'T2', 15, true, false, 'active',
 'metric_threshold', 'raffle_companion_count', 5, 'gte', NULL, 1, DATE '2026-07-13', 64,
 '职业陪跑', '陪跑满 5 场', '陪跑满五场,颁奖礼该给你发个纪念计数。', '常客未必是赢家,但一定是常客。'),

-- 元编年史(3):引用式,永不复制条件
(900040, 'meta_regular', NULL, NULL, '元编年史', 'T2', 15, false, false, 'active',
 'metric_threshold', 'achievement_unlock_total_count', 10, 'gte', NULL, 1, DATE '2026-07-13', 70,
 '熟客认证', '累计点亮成就满 10 枚', '十枚成就到手,你已经不算新人了。', NULL),
(900041, 'meta_category_onboarding', NULL, NULL, '元编年史', 'T3', 40, false, false, 'active',
 'meta_combo', NULL, NULL, NULL, '入职档案', 1, DATE '2026-07-13', 71,
 '档案齐全', '点亮「入职档案」全部成就', '入职档案填满,新人期正式结束。', NULL),
(900042, 'meta_series_master', NULL, NULL, '元编年史', 'T2', 15, false, false, 'active',
 'meta_combo', NULL, NULL, NULL, NULL, 1, DATE '2026-07-13', 72,
 '系列大师', '首次集齐任一系列全部已启用层级', '一整条系列全点亮,强迫症狂喜。', NULL);
