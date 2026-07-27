-- ═══════════════════════════════════════════════════════════════════════════
-- V15：成就体系按网吧世界观重命名（港风霓虹改造 Spec 4，2026-07-27）
--
-- 🚨 V9/V10 一个字节都不能改——Flyway 用 checksum 校验已应用的 migration，
--    改了生产直接启动失败。重命名一律走新增 migration。
--
-- 🚨 版本号为什么是 V15 而不是 V12：
--    Flyway 的 locations 是 5 个目录共享同一张 flyway_schema_history
--    （application.yml: activity,gallery,content,invoice,guide），版本号是**全局**的。
--    V1–V14 已全部占用：activity V1-V11 / gallery V2 / content V6 /
--    invoice V12,V14 / guide V13。用 V12 会直接撞 invoice。
--
-- 🚨 本迁移必须与以下代码改动**同批发布**，否则应用起不来或功能错乱：
--    ① AchievementContentWhitelist.ALLOWED_CATEGORIES —— 启动期白名单校验，
--       category 不在白名单内会 throw IllegalStateException 让应用启动失败
--    ② AchievementWallQueryService.META_CATEGORY —— 元老档案类目靠它筛出
--       metaAchievements[]，不同步会让该类目分组错乱
--    ③ activity-achievements/index.html 的 mapAchievementItem(it,'…') 与 aria-label
--    ④ RechargeAchievementJudgeJob 的 category 过滤串（"补给记录"→"充网费记录"）——
--       2026-07-28 全链矩阵测试抓出初版漏了它：旧串筛出空集，网费四条成就永不解锁。
--       类目名被代码当查询键的地方，换名时一个都不能漏（本文件 ⑤ 的 prerequisite 同教训）。
--
-- 幂等：全部是按 code / category 定位的 UPDATE，重跑无副作用。
-- ═══════════════════════════════════════════════════════════════════════════

-- ── ① 类目（8 启用 + 2 占位，与 AchievementContentWhitelist 白名单一一对应）──
UPDATE activity.achievement_definition SET category = '开卡入场'   WHERE category = '入职档案';
UPDATE activity.achievement_definition SET category = '上机记录'   WHERE category = '机房作业';
UPDATE activity.achievement_definition SET category = '充网费记录' WHERE category = '补给记录';
UPDATE activity.achievement_definition SET category = '全网联机'   WHERE category = '联动矩阵';
UPDATE activity.achievement_definition SET category = '画图机位'   WHERE category = '造像车间';
UPDATE activity.achievement_definition SET category = '上网时长'   WHERE category = '考勤本纪';
UPDATE activity.achievement_definition SET category = '隐藏关卡'   WHERE category = '机密档案';
UPDATE activity.achievement_definition SET category = '元老档案'   WHERE category = '元编年史';

-- ── ② 系列标题（10 条）──
UPDATE activity.achievement_series_label SET series_name = '局数系列 · SESSIONS'      WHERE series_code = 'api_calls';
UPDATE activity.achievement_series_label SET series_name = '网费系列 · TOP UP'        WHERE series_code = 'recharge_amount';
UPDATE activity.achievement_series_label SET series_name = '上机天数系列 · DAYS'      WHERE series_code = 'checkin_cum';
UPDATE activity.achievement_series_label SET series_name = '月度全勤系列 · FULL MONTH' WHERE series_code = 'checkin_full';
UPDATE activity.achievement_series_label SET series_name = '卡龄系列 · TENURE'        WHERE series_code = 'account_anniv';
UPDATE activity.achievement_series_label SET series_name = '扭蛋系列 · GACHA'         WHERE series_code = 'drawcard';
UPDATE activity.achievement_series_label SET series_name = '通宵榜系列 · LEADERBOARD' WHERE series_code = 'leaderboard';
UPDATE activity.achievement_series_label SET series_name = '带人系列 · REFERRAL'      WHERE series_code = 'referral';
UPDATE activity.achievement_series_label SET series_name = '出图系列 · IMAGE GEN'     WHERE series_code = 'image_gen';
-- appreciation 保持「鉴赏系列 · APPRECIATION」不变——鉴赏在两个世界观里都成立

-- ── ③ 42 条成就的 name 与 flavor_text ──
-- 按 code 定位（name 正是要改的东西，不能拿它当条件）。
-- 原文案的幽默调性有意保留：换的是世界观，不是风格。

-- 开卡入场(2)
UPDATE activity.achievement_definition SET name = '刷卡进门',
  flavor_text = '刷卡，机器亮了，今天算你到场。' WHERE code = 'checkin_first';
UPDATE activity.achievement_definition SET name = '第一局',
  flavor_text = '第一局开出去了，网管给你比了个大拇指。' WHERE code = 'api_first_call';

-- 上机记录(5)
UPDATE activity.achievement_definition SET name = '上道',
  flavor_text = '第 100 局，手生的劲儿总算过去了。' WHERE code = 'api_calls_1';
UPDATE activity.achievement_definition SET name = '老手',
  flavor_text = '一千局，你已经不用看键盘了。' WHERE code = 'api_calls_2';
UPDATE activity.achievement_definition SET name = '机王',
  flavor_text = '一万局，这台机子的风扇认得你的手速。' WHERE code = 'api_calls_3';
UPDATE activity.achievement_definition SET name = '一口气打穿',
  flavor_text = '一天干了别人一周的量，网管过来看了两眼。' WHERE code = 'api_daily_peak_1';
UPDATE activity.achievement_definition SET name = '文图双修',
  flavor_text = '文字机和画图机你都坐过，两头不亏待。' WHERE code = 'cross_surface_user';

-- 充网费记录(4)
UPDATE activity.achievement_definition SET name = '头一回充',
  flavor_text = '第一笔网费进账，账本上有你名字了。' WHERE code = 'recharge_amount_1';
UPDATE activity.achievement_definition SET name = '常客',
  flavor_text = '累计满一百，前台记住你常坐哪台。' WHERE code = 'recharge_amount_2';
UPDATE activity.achievement_definition SET name = '金主',
  flavor_text = '累计满五百，茶水间给你留了个杯子。' WHERE code = 'recharge_amount_3';
UPDATE activity.achievement_definition SET name = '月月来',
  flavor_text = '三个月月月都续，比很多正式工靠谱。' WHERE code = 'recharge_consistency_1';

-- 全网联机(9)
UPDATE activity.achievement_definition SET name = '摇过一把',
  flavor_text = '手一抖，扭蛋就出来了，爽感与手抖成正比。' WHERE code = 'drawcard_1';
UPDATE activity.achievement_definition SET name = '熟练摇臂手',
  flavor_text = '第十次摇，手感已经练出来了。' WHERE code = 'drawcard_2';
UPDATE activity.achievement_definition SET name = '检票入场',
  flavor_text = '检票口没拦你，你混进来了。' WHERE code = 'raffle_entry_1';
UPDATE activity.achievement_definition SET name = '天选时刻',
  flavor_text = '灯光扫到你这台机子，全场替你尖叫一声。' WHERE code = 'raffle_win_1';
UPDATE activity.achievement_definition SET name = '金票中人',
  flavor_text = '五十分之一，你就是那一。' WHERE code = 'gate_ticket_1';
UPDATE activity.achievement_definition SET name = '通宵榜留名',
  flavor_text = '前五十名，榜上有你了。' WHERE code = 'leaderboard_1';
UPDATE activity.achievement_definition SET name = '稳坐前排',
  flavor_text = '前十名，位置焐热了就没人抢得走。' WHERE code = 'leaderboard_2';
UPDATE activity.achievement_definition SET name = '带了个人来',
  flavor_text = '拉来第一个人，网吧多了一台机子在转。' WHERE code = 'referral_1';
UPDATE activity.achievement_definition SET name = '带队的',
  flavor_text = '三个人，够开一桌联机了。' WHERE code = 'referral_2';

-- 画图机位(5)
UPDATE activity.achievement_definition SET name = '第一张图',
  flavor_text = '第一张图跑出来了，像素都在颤抖。' WHERE code = 'image_gen_1';
UPDATE activity.achievement_definition SET name = '画图机位常客',
  flavor_text = '二十张图，这台画图机你坐熟了。' WHERE code = 'image_gen_2';
UPDATE activity.achievement_definition SET name = '出图机王',
  flavor_text = '一百张，出图速度比网速还快。' WHERE code = 'image_gen_3';
UPDATE activity.achievement_definition SET name = '懂行的',
  flavor_text = '点赞收藏二十次，审美这事儿你懂。' WHERE code = 'appreciation_1';
UPDATE activity.achievement_definition SET name = '策展人',
  flavor_text = '一百次，你的收藏夹快能办展了。' WHERE code = 'appreciation_2';

-- 上网时长(9)
UPDATE activity.achievement_definition SET name = '一周熟脸',
  flavor_text = '攒够七天，前台不用问你卡号了。' WHERE code = 'checkin_cum_1';
UPDATE activity.achievement_definition SET name = '固定机位',
  flavor_text = '三十天，这台机子算是你名下的了。' WHERE code = 'checkin_cum_2';
UPDATE activity.achievement_definition SET name = '常驻包夜',
  flavor_text = '一百天，拆迁都得绕开你这台。' WHERE code = 'checkin_cum_3';
UPDATE activity.achievement_definition SET name = '月度全勤',
  flavor_text = '一个月一天不落，新人里数你稳。' WHERE code = 'checkin_full_1';
UPDATE activity.achievement_definition SET name = '全勤老炮',
  flavor_text = '三个月满勤，这称呼不是白叫的。' WHERE code = 'checkin_full_2';
UPDATE activity.achievement_definition SET name = '百日常客',
  flavor_text = '认识满百天，网管开始跟你唠嗑了。' WHERE code = 'account_anniv_1';
UPDATE activity.achievement_definition SET name = '周年老会员',
  flavor_text = '一整年，你比很多会员卡的寿命都长。' WHERE code = 'account_anniv_2';
UPDATE activity.achievement_definition SET name = '零点上机',
  flavor_text = '零点整准时刷卡——闹钟辛苦了。' WHERE code = 'midnight_courier';
UPDATE activity.achievement_definition SET name = '回头客',
  flavor_text = '消失一个月，你的机位还给你留着。' WHERE code = 'ghost_return';

-- 隐藏关卡 / 其余
UPDATE activity.achievement_definition SET name = '通宵场',
  flavor_text = '凌晨的网吧，只剩你这台还亮着。' WHERE code = 'late_night_room';
UPDATE activity.achievement_definition SET name = '陪跑',
  flavor_text = '没中奖，但你确实来了，这也算数。' WHERE code = 'raffle_companion_1';
UPDATE activity.achievement_definition SET name = '职业陪跑',
  flavor_text = '陪跑满五场，该给你发个纪念币。' WHERE code = 'raffle_companion_2';

-- 元老档案(3)
UPDATE activity.achievement_definition SET name = '开业头一批',
  flavor_text = '开业那个月你就在场——绝版，不再版。' WHERE code = 'exclusive_founding_issue';
UPDATE activity.achievement_definition SET name = '开业月全勤',
  flavor_text = '开业首月一天没缺，这个章以后真印不出来了。' WHERE code = 'exclusive_founding_fullmonth';

-- 元成就（点亮其他成就后解锁）
UPDATE activity.achievement_definition SET name = '熟客认证',
  flavor_text = '十枚成就到手，你已经不算新人了。' WHERE code = 'meta_regular';
UPDATE activity.achievement_definition SET name = '开卡手续齐了',
  flavor_text = '开卡入场全点亮，新人期正式结束。' WHERE code = 'meta_category_onboarding';
UPDATE activity.achievement_definition SET name = '系列通关',
  flavor_text = '一整条系列打穿，这台机子被你摸透了。' WHERE code = 'meta_series_master';

-- ── ④ condition_text 里的旧世界观措辞 ──
UPDATE activity.achievement_definition
   SET condition_text = replace(condition_text, '点亮「入职档案」全部成就', '点亮「开卡入场」全部成就')
 WHERE condition_text LIKE '%入职档案%';

-- ── ⑤ prerequisite 列里的类目引用（2026-07-28 矩阵测试抓出的初版漏改）──
-- meta_category_onboarding 的判定引擎按 prerequisite 存的类目名现查该类目全部成就
-- （AchievementJudgeEngine.categoryFullyUnlocked）；① 改了 category 列却漏了这里，
-- 旧名筛出空集 → 「开卡手续齐了」永不解锁。全库 prerequisite 存类目名的只此一行
-- （V9:900041,即 meta_category_onboarding）,按 prerequisite 值定位即可精确幂等。
UPDATE activity.achievement_definition SET prerequisite = '开卡入场'
 WHERE prerequisite = '入职档案';
