-- ═══════════════════════════════════════════════════════════════════════════
-- V22：成就文案回退网吧化，恢复 V9/V10 原文（苹果式换代 批 5，2026-09-07）
--
-- 站长口径「文案换回原有的」＝逐条回到 V9（42 条成就 name/flavor_text + 8 类目
-- + prerequisite/condition_text 里的类目引用）与 V10（9 条系列标题）的原始值。
-- 本文件是 V15__achievement_wangba_rename.sql 的**逐条反向**：V15 有 61 条 UPDATE，
-- 这里就有 61 条，一一对应，值逐字对照 V9/V10 抄回（含 V9 的半角逗号/冒号写法）。
--
-- 🚨 V9/V10/V15 一个字节都不能改——Flyway 用 checksum 校验已应用的 migration，
--    改了生产直接启动失败。回退一律走新增 migration。
--
-- 🚨 版本号 V22：Flyway 的 locations 是 6 个目录共享同一张 flyway_schema_history
--    （application.yml: activity,gallery,content,invoice,guide,ops），版本号是**全局**的。
--    V1–V21 已全部占用（invoice V12/V14、guide V13、gallery V2、ops V20），V22 是下一个空位。
--
-- 🚨 本迁移必须与以下代码改动**同批发布**（V15 换名时的同一批同步点，方向相反）：
--    ① AchievementContentWhitelist.ALLOWED_CATEGORIES —— 启动期白名单校验，
--       category 不在白名单内会 throw IllegalStateException 让应用启动失败。
--       新旧两套值一直同时保留，本批只把「现行」的注释归属换回 V9 名。
--    ② AchievementWallQueryService.META_CATEGORIES —— 元编年史类目靠它筛出
--       metaAchievements[]（集合同认新旧两名，不同步会让该类目分组错乱）；
--       同文件还有用户可见角标 "曾是机密档案 #0X"。
--    ③ RechargeAchievementJudgeJob 的 category 过滤串（"充网费记录"→"补给记录"）——
--       2026-07-28 全链矩阵测试的教训：类目名被代码当查询键的地方换名一个都不能漏，
--       旧串筛出空集会让补给记录四条成就**永不解锁**。本批同样改为集合同认新旧两名。
--    ④ ai-relay deployment/files/activity-achievements/index.html 的 mapAchievementItem
--       类目键映射（本仓库之外，随活动页 rsync 发布，交批 4 处理）。
--
-- 幂等：全部是按 code / category / prerequisite 值定位的 UPDATE，重跑无副作用。
-- 不改表结构，不动任何行的 id / code / 判定字段（metric_code/threshold/comparator）。
-- ═══════════════════════════════════════════════════════════════════════════

-- ── ① 类目（8，回 V9 原名）──
UPDATE activity.achievement_definition SET category = '入职档案' WHERE category = '开卡入场';
UPDATE activity.achievement_definition SET category = '机房作业' WHERE category = '上机记录';
UPDATE activity.achievement_definition SET category = '补给记录' WHERE category = '充网费记录';
UPDATE activity.achievement_definition SET category = '联动矩阵' WHERE category = '全网联机';
UPDATE activity.achievement_definition SET category = '造像车间' WHERE category = '画图机位';
UPDATE activity.achievement_definition SET category = '考勤本纪' WHERE category = '上网时长';
UPDATE activity.achievement_definition SET category = '机密档案' WHERE category = '隐藏关卡';
UPDATE activity.achievement_definition SET category = '元编年史' WHERE category = '元老档案';

-- ── ② 系列标题（9，回 V10 原文）──
-- appreciation 的「鉴赏系列 · APPRECIATION」V15 就没改过，这里也无需回退。
UPDATE activity.achievement_series_label SET series_name = '调用量系列 · API CALLS'   WHERE series_code = 'api_calls';
UPDATE activity.achievement_series_label SET series_name = '金额系列 · RECHARGE'      WHERE series_code = 'recharge_amount';
UPDATE activity.achievement_series_label SET series_name = '累计出勤系列 · ATTENDANCE' WHERE series_code = 'checkin_cum';
UPDATE activity.achievement_series_label SET series_name = '满勤系列 · FULL MONTH'    WHERE series_code = 'checkin_full';
UPDATE activity.achievement_series_label SET series_name = '工龄系列 · TENURE'        WHERE series_code = 'account_anniv';
UPDATE activity.achievement_series_label SET series_name = '开卡系列 · DRAWCARD'      WHERE series_code = 'drawcard';
UPDATE activity.achievement_series_label SET series_name = '榜单系列 · LEADERBOARD'   WHERE series_code = 'leaderboard';
UPDATE activity.achievement_series_label SET series_name = '拉新系列 · REFERRAL'      WHERE series_code = 'referral';
UPDATE activity.achievement_series_label SET series_name = '造像系列 · IMAGE GEN'     WHERE series_code = 'image_gen';

-- ── ③ 42 条成就的 name 与 flavor_text（按 code 定位，值逐字回 V9；分组标题＝V9 类目）──
-- 入职档案(2)
UPDATE activity.achievement_definition SET name = '开机自检',
  flavor_text = '每天上班第一件事:证明我还活着。' WHERE code = 'checkin_first';
UPDATE activity.achievement_definition SET name = '首次握手',
  flavor_text = '握手成功,机房正式收编你。' WHERE code = 'api_first_call';

-- 机房作业(5)
UPDATE activity.achievement_definition SET name = '破壳',
  flavor_text = '第 100 次调用,壳裂了,人还在。' WHERE code = 'api_calls_1';
UPDATE activity.achievement_definition SET name = '满负荷',
  flavor_text = '负载曲线终于有了个像样的形状。' WHERE code = 'api_calls_2';
UPDATE activity.achievement_definition SET name = '重型机',
  flavor_text = '调用量破万,风扇开始有意见。' WHERE code = 'api_calls_3';
UPDATE activity.achievement_definition SET name = '爆机预警',
  flavor_text = '一天干了别人一周的活,机房记下了。' WHERE code = 'api_daily_peak_1';
UPDATE activity.achievement_definition SET name = '全栈选手',
  flavor_text = '文字模型和生图模型,你谁都不亏待。' WHERE code = 'cross_surface_user';

-- 补给记录(4)
UPDATE activity.achievement_definition SET name = '破冰',
  flavor_text = '第一笔钱进来,余额表破了处。' WHERE code = 'recharge_amount_1';
UPDATE activity.achievement_definition SET name = '常客',
  flavor_text = '累计满一百,前台已经记住你工位号。' WHERE code = 'recharge_amount_2';
UPDATE activity.achievement_definition SET name = '补给大户',
  flavor_text = '累计满五百,茶水间给你留了个杯子。' WHERE code = 'recharge_amount_3';
UPDATE activity.achievement_definition SET name = '细水长流',
  flavor_text = '三个月,月月都来,比很多正式工靠谱。' WHERE code = 'recharge_consistency_1';

-- 联动矩阵(9)
UPDATE activity.achievement_definition SET name = '开过卡',
  flavor_text = '手一抖,卡就开了,爽感与手抖成正比。' WHERE code = 'drawcard_1';
UPDATE activity.achievement_definition SET name = '熟练摇臂手',
  flavor_text = '第十次开卡,手感已经练出来了。' WHERE code = 'drawcard_2';
UPDATE activity.achievement_definition SET name = '检票入场',
  flavor_text = '检票口没拦你,你混进来了。' WHERE code = 'raffle_entry_1';
UPDATE activity.achievement_definition SET name = '天选时刻',
  flavor_text = '灯光扫到你工位,机房替你尖叫一声。' WHERE code = 'raffle_win_1';
UPDATE activity.achievement_definition SET name = '金票中人',
  flavor_text = '五十分之一,你就是那一。' WHERE code = 'gate_ticket_1';
UPDATE activity.achievement_definition SET name = '上榜留名',
  flavor_text = '前五十名,榜单上有你名字了。' WHERE code = 'leaderboard_1';
UPDATE activity.achievement_definition SET name = '稳坐钓鱼台',
  flavor_text = '前十名,鱼竿一收,位置还在。' WHERE code = 'leaderboard_2';
UPDATE activity.achievement_definition SET name = '拉新新手',
  flavor_text = '拉来第一个人,机房人口+1。' WHERE code = 'referral_1';
UPDATE activity.achievement_definition SET name = '拉新达人',
  flavor_text = '三个人,够组一支小队了。' WHERE code = 'referral_2';

-- 造像车间(5)
UPDATE activity.achievement_definition SET name = '第一张图',
  flavor_text = '第一张图跑出来了,像素都在颤抖。' WHERE code = 'image_gen_1';
UPDATE activity.achievement_definition SET name = '开工大吉',
  flavor_text = '二十张图,车间正式投产。' WHERE code = 'image_gen_2';
UPDATE activity.achievement_definition SET name = '产能拉满',
  flavor_text = '一百张,产线连轴转都追不上你。' WHERE code = 'image_gen_3';
UPDATE activity.achievement_definition SET name = '懂鉴赏',
  flavor_text = '点赞收藏二十次,审美这事儿你懂。' WHERE code = 'appreciation_1';
UPDATE activity.achievement_definition SET name = '策展人',
  flavor_text = '一百次,你的收藏夹快能办展了。' WHERE code = 'appreciation_2';

-- 考勤本纪(9)
UPDATE activity.achievement_definition SET name = '打卡新人',
  flavor_text = '攒够七天,考勤簿认识你了。' WHERE code = 'checkin_cum_1';
UPDATE activity.achievement_definition SET name = '常驻工位',
  flavor_text = '三十天,工位已经算你名下的了。' WHERE code = 'checkin_cum_2';
UPDATE activity.achievement_definition SET name = '机房钉子户',
  flavor_text = '一百天,拆迁都得绕开你。' WHERE code = 'checkin_cum_3';
UPDATE activity.achievement_definition SET name = '全勤新兵',
  flavor_text = '一个月一天不落,新兵蛋子干得漂亮。' WHERE code = 'checkin_full_1';
UPDATE activity.achievement_definition SET name = '全勤老兵',
  flavor_text = '三个月满勤,老兵这称呼不是白叫的。' WHERE code = 'checkin_full_2';
UPDATE activity.achievement_definition SET name = '百日报到',
  flavor_text = '认识满百天,机房开始对你有感情了。' WHERE code = 'account_anniv_1';
UPDATE activity.achievement_definition SET name = '周年机长',
  flavor_text = '一整年,你比很多工牌寿命都长。' WHERE code = 'account_anniv_2';
UPDATE activity.achievement_definition SET name = '创刊号',
  flavor_text = '考勤簿开簿那个月,你在场——绝版,不再版。' WHERE code = 'exclusive_founding_issue';
UPDATE activity.achievement_definition SET name = '元年全勤',
  flavor_text = '开簿首月一天没缺,这个印章以后真印不出来了。' WHERE code = 'exclusive_founding_fullmonth';

-- 机密档案(5)
UPDATE activity.achievement_definition SET name = '零点信使',
  flavor_text = '零点整准时打卡——闹钟辛苦了。' WHERE code = 'midnight_courier';
UPDATE activity.achievement_definition SET name = '深夜机房',
  flavor_text = '凌晨的机房,只有风扇和你还醒着。' WHERE code = 'late_night_room';
UPDATE activity.achievement_definition SET name = '诈尸打卡',
  flavor_text = '消失一个月,考勤簿还是给你留了格。' WHERE code = 'ghost_return';
UPDATE activity.achievement_definition SET name = '陪跑',
  flavor_text = '没中奖,但你确实来了,这也算数。' WHERE code = 'raffle_companion_1';
UPDATE activity.achievement_definition SET name = '职业陪跑',
  flavor_text = '陪跑满五场,颁奖礼该给你发个纪念计数。' WHERE code = 'raffle_companion_2';

-- 元编年史(3)
UPDATE activity.achievement_definition SET name = '熟客认证',
  flavor_text = '十枚成就到手,你已经不算新人了。' WHERE code = 'meta_regular';
UPDATE activity.achievement_definition SET name = '档案齐全',
  flavor_text = '入职档案填满,新人期正式结束。' WHERE code = 'meta_category_onboarding';
UPDATE activity.achievement_definition SET name = '系列大师',
  flavor_text = '一整条系列全点亮,强迫症狂喜。' WHERE code = 'meta_series_master';
-- ── ④ condition_text 里的类目引用（回 V9 原文）──
UPDATE activity.achievement_definition
   SET condition_text = replace(condition_text, '点亮「开卡入场」全部成就', '点亮「入职档案」全部成就')
 WHERE condition_text LIKE '%开卡入场%';

-- ── ⑤ prerequisite 列里的类目引用（全库唯一一行：V9:900041 meta_category_onboarding）──
-- 判定引擎 AchievementJudgeEngine.categoryFullyUnlocked 按这一列存的类目名现查该类目
-- 全部成就；只回退 ① 的 category 列而漏了这里，「档案齐全」会永不解锁（V15 初版原样踩过）。
UPDATE activity.achievement_definition SET prerequisite = '入职档案'
 WHERE prerequisite = '开卡入场';
