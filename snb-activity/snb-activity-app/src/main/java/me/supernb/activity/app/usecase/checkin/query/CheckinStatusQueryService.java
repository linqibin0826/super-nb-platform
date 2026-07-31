package me.supernb.activity.app.usecase.checkin.query;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.supernb.activity.app.usecase.checkin.CheckinEntryGateChecker;
import me.supernb.activity.app.usecase.checkin.config.CheckinBalanceProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinTierProperties;
import me.supernb.activity.domain.model.checkin.CheckinEntryGate;
import me.supernb.activity.domain.model.checkin.CheckinEntryGateView;
import me.supernb.activity.domain.model.checkin.CheckinMilestoneView;
import me.supernb.activity.domain.model.checkin.CheckinStatusView;
import me.supernb.activity.domain.model.checkin.CheckinStreak;
import me.supernb.activity.domain.model.checkin.CheckinSupplyTierView;
import me.supernb.activity.domain.model.checkin.CheckinSupplyView;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardCalc;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardRecord;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardView;
import me.supernb.activity.domain.port.checkin.CheckinDailyRewardPort;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.nb.NbLedgerPort;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.springframework.stereotype.Service;

/// 签到状态查询(spec §7.3;字段形状按前端接线计划契约总览钉死)。业务文案(statusText/
/// gaugeNote/state)一律服务端算好给成品字符串,自然日/时区/资格判定全在服务端(Asia/Shanghai
/// 显式),前端不得用 `new Date()` 重新推导"今天是几号"。
/// 加时资格 2026-07-31 起改为「当月累计签满 N 天」,不再有"上线日分母"这回事。
@Service
public class CheckinStatusQueryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int STREAK_LOOKBACK_DAYS = 60;
    private static final Duration MIN_ACCOUNT_AGE = Duration.ofHours(24);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy.MM");

    private final CheckinPort checkinPort;
    private final CheckinRechargeReadPort rechargePort;
    private final AccountRegistrationReadPort registrationPort;
    private final NbLedgerPort nbLedger;
    private final CheckinProperties props;
    private final CheckinTierProperties tierProps;
    private final CheckinSettlementProperties settlementProps;
    private final CheckinDailyRewardPort dailyRewardPort;
    private final CheckinBalanceProperties balanceProps;
    private final CheckinEntryGateChecker entryGate;

    /// 构造:注入签到端口、补给充值读端口、账龄读端口、NB 账本读端口、日返网费台账端口、
    /// 四个配置类(settlementProps 提供加时资格的当月累计出勤门槛,与月度结算 job 同一真源;
    /// balanceProps 提供返网费单价/门槛/总闸)与准入闸判定器(与打卡命令共用同一真源)。
    public CheckinStatusQueryService(CheckinPort checkinPort, CheckinRechargeReadPort rechargePort,
            AccountRegistrationReadPort registrationPort, NbLedgerPort nbLedger, CheckinProperties props,
            CheckinTierProperties tierProps, CheckinSettlementProperties settlementProps,
            CheckinDailyRewardPort dailyRewardPort, CheckinBalanceProperties balanceProps,
            CheckinEntryGateChecker entryGate) {
        this.checkinPort = checkinPort;
        this.rechargePort = rechargePort;
        this.registrationPort = registrationPort;
        this.nbLedger = nbLedger;
        this.props = props;
        this.tierProps = tierProps;
        this.settlementProps = settlementProps;
        this.dailyRewardPort = dailyRewardPort;
        this.balanceProps = balanceProps;
        this.entryGate = entryGate;
    }

    /// 组装某用户的签到状态视图(生产入口:今天 = Asia/Shanghai 当前自然日)。
    public CheckinStatusView status(long userId) {
        return statusAt(userId, LocalDate.now(ZONE), Instant.now());
    }

    /// 显式基准时间的重载,供单测锁定「今天是几号」(跨月/月末等分支靠它才测得到)。
    /// 包私有,不对外暴露——照本类 buildMilestones「依赖今天的计算抽成显式接收 today 的
    /// 纯函数」同款先例。
    CheckinStatusView statusAt(long userId, LocalDate today, Instant now) {
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        boolean eligible = registrationPort.registeredAt(userId)
                .map(at -> at.isBefore(now.minus(MIN_ACCOUNT_AGE)))
                .orElse(false);
        String ineligibleReason = eligible ? null : "account_too_new";

        // 准入闸(spec §12):账龄过了才轮到查充值(太新的号连闸都不用开)。闸门未启用时
        // entryGateView 恒 null,前端据此走旧行为。锁态下页面照常渲染(计价梯当橱窗),
        // 只是 eligible=false 让打卡不可用。
        CheckinEntryGateView entryGateView = null;
        if (eligible && entryGate.enabled()) {
            CheckinEntryGate.Result gate = entryGate.check(userId, today);
            entryGateView = buildEntryGateView(gate);
            if (!gate.eligible()) {
                eligible = false;
                ineligibleReason = "recharge_required";
            }
        }

        boolean punchedToday = checkinPort.checkedInOn(userId, today);
        var monthDates = checkinPort.datesInRange(userId, monthStart, monthEnd);
        List<Integer> checkedDays = monthDates.stream().map(LocalDate::getDayOfMonth).sorted().toList();
        int monthCount = monthDates.size();
        int cumulativeDays = checkinPort.totalCheckins(userId);

        var recentDates = checkinPort.datesInRange(userId, today.minusDays(STREAK_LOOKBACK_DAYS), today);
        int streakCurrent = CheckinStreak.current(recentDates, today);

        // 加时资格在轨 = 本月**累计**签到已达门槛(2026-07-31 起不再要求一天不落)。
        // 旧口径「自上线日起逐日回溯判在轨」已随满勤退役——它命中率仅 1%,且一断签就整月无望。
        boolean onTrackFullMonth = monthCount >= settlementProps.fullMonthDays();
        List<CheckinMilestoneView> milestones = buildMilestones(monthCount, settlementProps.fullMonthDays());

        Instant monthStartInstant = monthStart.atStartOfDay(ZONE).toInstant();
        Instant nextMonthStartInstant = monthStart.plusMonths(1).atStartOfDay(ZONE).toInstant();
        BigDecimal monthlyRecharge = rechargePort.monthlyRecharge(userId, monthStartInstant, nextMonthStartInstant);
        CheckinSupplyView supply = buildSupply(monthlyRecharge, onTrackFullMonth);

        int nbTotal = nbLedger.totalPoints(userId);
        CheckinDailyRewardView dailyReward =
                buildDailyReward(userId, today, now, monthStart, monthEnd, monthDates, punchedToday);
        return new CheckinStatusView(eligible, ineligibleReason, punchedToday, today.getDayOfMonth(),
                today.format(MONTH_LABEL), today.lengthOfMonth(), checkedDays, cumulativeDays, streakCurrent,
                milestones, supply, nbTotal, dailyReward, entryGateView);
    }

    /// 组装准入闸视图,成品文案在此定稿(前端不得自行拼日期算术):
    /// 锁态「近 30 天已充 ¥X / 还差 ¥Y」给足下一步动作;开启态「网费还够 N 天」把滚动窗口
    /// 的暗坑变成明牌——旧充值滑出窗口连签会无预警断掉,提前亮出来跟网吧包时卡一个逻辑。
    private CheckinEntryGateView buildEntryGateView(CheckinEntryGate.Result gate) {
        String noteText;
        if (gate.eligible()) {
            noteText = gate.remainingDays() == 1
                    ? "网费今天到期,记得续"
                    : "网费还够 " + gate.remainingDays() + " 天";
        } else {
            BigDecimal shortfall = entryGate.minCny().subtract(gate.rechargedCny());
            noteText = "近 " + entryGate.windowDays() + " 天已充 ¥"
                    + CheckinEntryGateChecker.plain(gate.rechargedCny())
                    + " / 还差 ¥" + CheckinEntryGateChecker.plain(shortfall);
        }
        return new CheckinEntryGateView(gate.eligible(), entryGate.minCny(), entryGate.windowDays(),
                gate.rechargedCny(), gate.remainingDays(), noteText);
    }

    /// 组装连签阶梯:今天档位 / 明天档位 / 门槛态 / 本月已返累计。
    ///
    /// ⚠️ **明天档位的口径最易写错**:今天**未签**时,今天这条连签已经断了——明天签到只能是
    /// 本月第 1 天,绝不能恒写成 `streakDay + 1`,那会给未签用户画一个他根本拿不到的高档位。
    /// 明天跨月同理归 1(自然月清零)。
    private CheckinDailyRewardView buildDailyReward(long userId, LocalDate today, Instant now,
            LocalDate monthStart, LocalDate monthEnd, List<LocalDate> monthDates, boolean punchedToday) {
        int streakDay = CheckinDailyRewardCalc.streakDay(monthDates, today);
        boolean balanceEligible = rechargePort.lifetimeRecharge(userId, now)
                .compareTo(balanceProps.thresholdCny()) >= 0;
        boolean payable = balanceEligible && balanceProps.enabled();

        LocalDate tomorrow = today.plusDays(1);
        int tomorrowStreakDay =
                (tomorrow.getMonthValue() == today.getMonthValue() && punchedToday) ? streakDay + 1 : 1;

        String todayBalanceStatus = dailyRewardPort.findByUserAndDay(userId, today)
                .map(CheckinDailyRewardRecord::balanceStatus)
                .orElse("not_punched");

        return new CheckinDailyRewardView(
                streakDay,
                payable
                        ? CheckinDailyRewardCalc.balanceCny(streakDay, balanceProps.perDayCny(),
                                balanceProps.stepDays())
                        : BigDecimal.ZERO,
                CheckinDailyRewardCalc.nbPoints(streakDay, props.dailyNbPoints()),
                todayBalanceStatus,
                tomorrowStreakDay,
                payable
                        ? CheckinDailyRewardCalc.balanceCny(tomorrowStreakDay, balanceProps.perDayCny(),
                                balanceProps.stepDays())
                        : BigDecimal.ZERO,
                CheckinDailyRewardCalc.nbPoints(tomorrowStreakDay, props.dailyNbPoints()),
                balanceEligible,
                balanceEligible ? null
                        : "累计充值满 ¥" + balanceProps.thresholdCny().stripTrailingZeros().toPlainString()
                                + " 解锁返网费",
                dailyRewardPort.myMonthlyBalanceTotal(userId, monthStart, monthEnd),
                balanceProps.perDayCny(),
                balanceProps.stepDays());
    }

    /// 组装四档里程碑(出勤 5/10/20 三枚徽章 + 加时资格),**按 target 升序**、成品状态文案。
    ///
    /// 2026-07-31 起「加时资格」由「满勤(一天不落且今天已是月末)」改为「当月累计签满
    /// fullMonthDays 天」——不再依赖"今天是不是月末",月中达标即亮,断签的人追赶累计天数
    /// 仍有奔头。旧口径命中率仅 1%(7 月 212 位打卡者中 2 人),等于 99% 的人看着一个永远
    /// 达不成的格子。
    ///
    /// 🚨 顺序必须按 target 排而不能写死:门槛是 env 可调的(同日已从 20 调到 15),一旦它
    /// 落到 5/10/20 三枚徽章**中间**,固定排第四就会让 rail 读成「20 天:15/20 未达」紧挨着
    /// 「加时资格:已打穿」——后面那格先亮、前面那格还没到。稳定排序保证同值时徽章在前。
    /// 包私有(非 private)以便脱离端口 mock 直接单测这一纯计算(仿 `BoardPeriods` 先例)。
    static List<CheckinMilestoneView> buildMilestones(int monthCount, int fullMonthDays) {
        List<CheckinMilestoneView> list = new ArrayList<>();
        list.add(milestoneOf("days_5", "出勤 5 天", 5, monthCount));
        list.add(milestoneOf("days_10", "出勤 10 天", 10, monthCount));
        list.add(milestoneOf("days_20", "出勤 20 天", 20, monthCount));
        boolean fullMonthAchieved = monthCount >= fullMonthDays;
        String fullMonthText = fullMonthAchieved ? "已打穿" : (monthCount + " / " + fullMonthDays);
        list.add(new CheckinMilestoneView("full_month", "加时资格", fullMonthDays, fullMonthAchieved,
                fullMonthText));
        list.sort(Comparator.comparingInt(CheckinMilestoneView::target)); // List.sort 稳定
        return list;
    }

    private static CheckinMilestoneView milestoneOf(String code, String label, int target, int monthCount) {
        boolean achieved = monthCount >= target;
        String statusText = achieved ? "已打穿" : (monthCount + " / " + target);
        return new CheckinMilestoneView(code, label, target, achieved, statusText);
    }

    /// 组装补给三档进度:armed(已达标)/progress(下一个未达标目标档)/dim(远未达标)。
    /// gaugePct 用分段刻度公式(见 [#gaugePctBySegmentedScale]),gaugeNote 仍面向"下一个未达标档"
    /// 给出成品文案——已达标档的 statusText 同时提示满勤是否在轨(充值达标不代表已发放,
    /// 发放仍需月末满勤复核)。
    private CheckinSupplyView buildSupply(BigDecimal monthlyRecharge, boolean onTrackFullMonth) {
        List<CheckinTierProperties.TierInfo> infos = tierProps.tiers();
        CheckinTierProperties.TierInfo nextUnmet = infos.stream()
                .filter(t -> monthlyRecharge.compareTo(t.threshold()) < 0)
                .findFirst().orElse(null);

        List<CheckinSupplyTierView> tiers = new ArrayList<>();
        for (CheckinTierProperties.TierInfo t : infos) {
            boolean armed = monthlyRecharge.compareTo(t.threshold()) >= 0;
            String state;
            String statusText;
            if (armed) {
                state = "armed";
                statusText = onTrackFullMonth ? "充值已达标 · 出勤在轨" : "充值已达标 · 出勤未达标";
            } else if (nextUnmet != null && nextUnmet.tier().equals(t.tier())) {
                state = "progress";
                BigDecimal gap = t.threshold().subtract(monthlyRecharge).stripTrailingZeros();
                statusText = "差 ¥" + gap.toPlainString();
            } else {
                state = "dim";
                statusText = "未在轨";
            }
            tiers.add(new CheckinSupplyTierView(t.tier(), t.label(), t.conditionText(), t.threshold(),
                    state, statusText));
        }

        int gaugePct = gaugePctBySegmentedScale(monthlyRecharge, infos);
        String gaugeNote;
        if (nextUnmet == null) {
            gaugeNote = "已全部达标";
        } else {
            BigDecimal gap = nextUnmet.threshold().subtract(monthlyRecharge).stripTrailingZeros();
            gaugeNote = "距 " + nextUnmet.tier() + " 档还差 ¥" + gap.toPlainString();
        }

        return new CheckinSupplyView(monthlyRecharge, gaugePct, gaugeNote, tiers);
    }

    /// gaugePct 分段刻度公式(2026-07-14 控制器裁决,覆盖 spec 草稿"朝下一档线性"公式,
    /// 对齐前端仪表盘刻度视觉):三档阈值 A/B/C 依次立于刻度 33%/66%/100%,起点 0 立于 0%,
    /// 段内线性插值,四舍五入取整。¥36(A=30,B=50)→33+(36-30)/(50-30)×33=42.9→43,
    /// 与前端契约示例 JSON 吻合。infos 恒为 [A,B,C] 三个元素(阈值升序,CheckinTierProperties
    /// 构造时固定生成),故按下标直取而非再次查找。
    ///
    /// 每段另封顶(2026-07-14 复审裁决,消除"满格却未达标"矛盾):四舍五入不得把尚未真正
    /// 达标的金额显示成下一档的整格刻度(33/66/100)——例如 ¥495 距 C 档(¥500)还差 ¥5,
    /// tiers[].state 仍是"progress"、statusText 仍是"差 ¥5",但 round(99.62)=100 会让
    /// 进度条视觉满格,与"未达标"文案自相矛盾;故本段封顶到刻度线之下的整数(32/65/99),
    /// 只有 amount 真正达到阈值(落入下一分支或直接 return 100)才允许显示满格。
    private static int gaugePctBySegmentedScale(BigDecimal amount, List<CheckinTierProperties.TierInfo> infos) {
        double amt = amount.doubleValue();
        double a = infos.get(0).threshold().doubleValue();
        double b = infos.get(1).threshold().doubleValue();
        double c = infos.get(2).threshold().doubleValue();
        if (amt < a) {
            return cappedRound(amt / a * 33, 32);
        } else if (amt < b) {
            return cappedRound(33 + (amt - a) / (b - a) * 33, 65);
        } else if (amt < c) {
            return cappedRound(66 + (amt - b) / (c - b) * 34, 99);
        } else {
            return 100;
        }
    }

    /// 四舍五入取整后夹到 [0, segmentCap],segmentCap 是当前段允许显示的最高刻度(比下一档的
    /// 整格刻度小 1),防止四舍五入把未达标金额"抬"到下一档的满格视觉。
    private static int cappedRound(double pct, int segmentCap) {
        int rounded = (int) Math.round(pct);
        return Math.max(0, Math.min(segmentCap, rounded));
    }
}
