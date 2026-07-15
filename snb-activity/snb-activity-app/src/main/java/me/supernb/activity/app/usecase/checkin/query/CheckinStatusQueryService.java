package me.supernb.activity.app.usecase.checkin.query;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinTierProperties;
import me.supernb.activity.domain.model.checkin.CheckinMilestoneView;
import me.supernb.activity.domain.model.checkin.CheckinStatusView;
import me.supernb.activity.domain.model.checkin.CheckinStreak;
import me.supernb.activity.domain.model.checkin.CheckinSupplyTierView;
import me.supernb.activity.domain.model.checkin.CheckinSupplyView;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.nb.NbLedgerPort;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.springframework.stereotype.Service;

/// 签到状态查询(spec §7.3;字段形状按前端接线计划契约总览钉死)。业务文案(statusText/
/// gaugeNote/state)一律服务端算好给成品字符串,自然日/时区/满勤判定全在服务端(Asia/Shanghai
/// 显式),前端不得用 `new Date()` 重新推导"今天是几号"。满勤分母显式限定"仅计上线日之后"
/// (红一④/红二9),历史记录不计入分母。
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

    /// 构造:注入签到端口、补给充值读端口、账龄读端口、NB 账本读端口与两个配置类。
    public CheckinStatusQueryService(CheckinPort checkinPort, CheckinRechargeReadPort rechargePort,
            AccountRegistrationReadPort registrationPort, NbLedgerPort nbLedger, CheckinProperties props,
            CheckinTierProperties tierProps) {
        this.checkinPort = checkinPort;
        this.rechargePort = rechargePort;
        this.registrationPort = registrationPort;
        this.nbLedger = nbLedger;
        this.props = props;
        this.tierProps = tierProps;
    }

    /// 组装某用户的签到状态视图。
    public CheckinStatusView status(long userId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        Instant now = Instant.now();

        boolean eligible = registrationPort.registeredAt(userId)
                .map(at -> at.isBefore(now.minus(MIN_ACCOUNT_AGE)))
                .orElse(false);
        String ineligibleReason = eligible ? null : "account_too_new";

        boolean punchedToday = checkinPort.checkedInOn(userId, today);
        var monthDates = checkinPort.datesInRange(userId, monthStart, monthEnd);
        List<Integer> checkedDays = monthDates.stream().map(LocalDate::getDayOfMonth).sorted().toList();
        int monthCount = monthDates.size();
        int cumulativeDays = checkinPort.totalCheckins(userId);

        var recentDates = checkinPort.datesInRange(userId, today.minusDays(STREAK_LOOKBACK_DAYS), today);
        int streakCurrent = CheckinStreak.current(recentDates, today);

        // 满勤在轨:自"月初与上线日两者取晚"起到今天为止一天不落——月中动态展示"目前在线全勤",
        // 月末(今天是本月最后一天 且在轨)即成为最终"已打穿"判定,判定细节见 buildMilestones。
        LocalDate fullMonthWindowStart = monthStart.isAfter(props.launchDate()) ? monthStart : props.launchDate();
        boolean onTrackFullMonth;
        if (fullMonthWindowStart.isAfter(today)) {
            onTrackFullMonth = false;
        } else {
            long expectedDays = ChronoUnit.DAYS.between(fullMonthWindowStart, today) + 1;
            int countSinceWindowStart = checkinPort.countInRange(userId, fullMonthWindowStart, today);
            onTrackFullMonth = countSinceWindowStart >= expectedDays;
        }
        List<CheckinMilestoneView> milestones = buildMilestones(monthCount, onTrackFullMonth, today);

        Instant monthStartInstant = monthStart.atStartOfDay(ZONE).toInstant();
        Instant nextMonthStartInstant = monthStart.plusMonths(1).atStartOfDay(ZONE).toInstant();
        BigDecimal monthlyRecharge = rechargePort.monthlyRecharge(userId, monthStartInstant, nextMonthStartInstant);
        CheckinSupplyView supply = buildSupply(monthlyRecharge, onTrackFullMonth);

        int nbTotal = nbLedger.totalPoints(userId);
        return new CheckinStatusView(eligible, ineligibleReason, punchedToday, today.getDayOfMonth(),
                today.format(MONTH_LABEL), today.lengthOfMonth(), checkedDays, cumulativeDays, streakCurrent,
                milestones, supply, nbTotal);
    }

    /// 组装四档里程碑(5/10/20/满勤),固定顺序、成品状态文案。
    ///
    /// 满勤达成 = 在轨 **且** 今天已是本月最后一天;不能再用 monthCount(整月签到数)与
    /// monthDays(全月天数)的大小比较——上线日若落在被测月中旬,monthCount 物理上不可能追上
    /// monthDays(如 7/13 上线、7/31 共 19 天签到 vs 31 天分母),会导致满勤永远判定不通过
    /// (2026-07-14 复审修复,呼应 spec 红一④"满勤仅计上线日之后"的分母调整不能反被终判抵消)。
    /// 包私有(非 private)以便脱离 `LocalDate.now()`/端口 mock,直接用固定 `today` 单测这一纯计算
    /// (仿 `BoardPeriods` 先例:依赖"今天"的计算抽成显式接收 today/now 的纯函数)。
    static List<CheckinMilestoneView> buildMilestones(int monthCount, boolean onTrackFullMonth, LocalDate today) {
        int monthDays = today.lengthOfMonth();
        List<CheckinMilestoneView> list = new ArrayList<>();
        list.add(milestoneOf("days_5", "出勤 5 天", 5, monthCount));
        list.add(milestoneOf("days_10", "出勤 10 天", 10, monthCount));
        list.add(milestoneOf("days_20", "出勤 20 天", 20, monthCount));
        boolean fullMonthAchieved = onTrackFullMonth && today.getDayOfMonth() == monthDays;
        String fullMonthText = fullMonthAchieved ? "已打穿" : (onTrackFullMonth ? "在轨 · 一格没漏" : "本月已错过");
        list.add(new CheckinMilestoneView("full_month", "满勤", monthDays, fullMonthAchieved, fullMonthText));
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
                statusText = onTrackFullMonth ? "充值已达标 · 满勤在轨" : "充值已达标 · 满勤未达标";
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
