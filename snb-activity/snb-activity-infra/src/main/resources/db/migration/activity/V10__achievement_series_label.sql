-- V10:系列人话标题小型内容表。系列 code 与 achievement_definition.series_code 对应,
-- 5 个抄深化稿 §3 各系列小节标题(api_calls/recharge_amount/checkin_cum/checkin_full/
-- account_anniv),另 5 个(drawcard/leaderboard/referral/image_gen/appreciation)深化稿
-- 未给标题,本表按同一语域自拟——内容假设,后续改一条 UPDATE 即可,不碰代码/不碰发版节奏。
CREATE TABLE activity.achievement_series_label (
    series_code TEXT PRIMARY KEY,
    series_name TEXT NOT NULL
);

INSERT INTO activity.achievement_series_label (series_code, series_name) VALUES
('api_calls', '调用量系列 · API CALLS'),
('recharge_amount', '金额系列 · RECHARGE'),
('checkin_cum', '累计出勤系列 · ATTENDANCE'),
('checkin_full', '满勤系列 · FULL MONTH'),
('account_anniv', '工龄系列 · TENURE'),
('drawcard', '开卡系列 · DRAWCARD'),
('leaderboard', '榜单系列 · LEADERBOARD'),
('referral', '拉新系列 · REFERRAL'),
('image_gen', '造像系列 · IMAGE GEN'),
('appreciation', '鉴赏系列 · APPRECIATION');
