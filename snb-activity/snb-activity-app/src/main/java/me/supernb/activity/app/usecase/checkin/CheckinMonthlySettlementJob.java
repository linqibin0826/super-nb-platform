package me.supernb.activity.app.usecase.checkin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinTierProperties;
import me.supernb.activity.domain.model.checkin.CheckinRewardCandidate;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.checkin.CheckinRewardPort;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 补给资格月度结算(spec §7.4):次月 1 日 01:05(Asia/Shanghai)为"上个月"结算——满勤这件事
/// 只有月末才能盖棺定论,天然是月度批处理而非实时判定。①先重试遗留的 pending/failed
/// (attempts<3)行(job 崩溃恢复,原样按已占位的 tier/group-id/notes 重发,不重新判定资格);
/// ②扫描新候选(满勤∧当月新增充值达标)→按 (user_id,grant_month) 原子占位→按 userId 升序稳定
/// 排序分配预算,超顶转 deferred(该自然月终态,不被本 job 在未来月份追认补发)→按档分批调用
/// bulk-assign。整批 HTTP 调用失败重试 ≤3 次(仅传输层异常);单个用户的业务级失败(如分组冲突)
/// 不在本次调用内重试,交给下一次运行的遗留重试分支处理。scanEnabled=false 或
/// tierRewardEnabled=false 时整体跳过(spec §6/§7.7 硬阻断)。
@Slf4j
@Service
public class CheckinMonthlySettlementJob {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter NOTES_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MAX_TRANSPORT_RETRIES = 3;

    private final CheckinPort checkinPort;
    private final CheckinRechargeReadPort rechargePort;
    private final CheckinRewardPort rewardPort;
    private final SubscriptionGrantPort grantPort;
    private final CheckinProperties props;
    private final CheckinTierProperties tierProps;
    private final CheckinSettlementProperties settlementProps;

    /// Spring 装配构造:SubscriptionGrantPort 的唯一实现(SubscriptionGrantAdapter)
    /// `@ConditionalOnProperty(sub2api.admin-key)`,未配 admin-key 的环境里该 Bean 根本不存在
    /// (A-13 教训)。用 ObjectProvider 取值——它本身永远可注入,`getIfAvailable()` 在无 Bean 时
    /// 返回 null 而不抛异常,绝不能让"无 admin-key 环境"因本 job 直接注入 SubscriptionGrantPort
    /// 而导致整个 Spring 上下文启动失败。
    @Autowired
    public CheckinMonthlySettlementJob(CheckinPort checkinPort, CheckinRechargeReadPort rechargePort,
            CheckinRewardPort rewardPort, ObjectProvider<SubscriptionGrantPort> grantPortProvider,
            CheckinProperties props, CheckinTierProperties tierProps,
            CheckinSettlementProperties settlementProps) {
        this(checkinPort, rechargePort, rewardPort, grantPortProvider.getIfAvailable(), props, tierProps,
                settlementProps);
    }

    /// 全参构造(测试直接注入 mock/null grantPort,照 RaffleDrawJob 双构造器惯例)。
    CheckinMonthlySettlementJob(CheckinPort checkinPort, CheckinRechargeReadPort rechargePort,
            CheckinRewardPort rewardPort, SubscriptionGrantPort grantPort, CheckinProperties props,
            CheckinTierProperties tierProps, CheckinSettlementProperties settlementProps) {
        this.checkinPort = checkinPort;
        this.rechargePort = rechargePort;
        this.rewardPort = rewardPort;
        this.grantPort = grantPort;
        this.props = props;
        this.tierProps = tierProps;
        this.settlementProps = settlementProps;
    }

    /// 每月 1 日 01:05(Asia/Shanghai)结算入口。
    @Scheduled(cron = "0 5 1 1 * *", zone = "Asia/Shanghai")
    public void settlePreviousMonth() {
        if (!settlementProps.scanEnabled() || !settlementProps.tierRewardEnabled()) {
            log.info("补给资格月度结算已跳过:scanEnabled={} tierRewardEnabled={}",
                    settlementProps.scanEnabled(), settlementProps.tierRewardEnabled());
            return;
        }
        if (grantPort == null) {
            // 理论上不应发生:tierRewardEnabled 硬开关默认 false,与 admin-key 配置本应同步打开。
            // 此处仅作最后一道防线——绝不能在 grantPort 缺失时继续跑到发放调用抛 NPE。
            log.error("补给资格月度结算已跳过:SubscriptionGrantPort 未装配(检查 sub2api.admin-key 配置)");
            return;
        }
        YearMonth lastMonth = YearMonth.now(ZONE).minusMonths(1);
        try {
            retryLeftovers();
        } catch (Exception e) {
            log.error("补给资格结算——遗留重试阶段异常", e);
        }
        try {
            claimAndSendNewCandidates(lastMonth);
        } catch (Exception e) {
            log.error("补给资格结算——新候选发放阶段异常 month={}", lastMonth, e);
        }
    }

    /// 重试上一次运行遗留的 pending(崩溃恢复)与 failed(attempts<3)行,原样按已占位的
    /// tier/group-id/notes 重发,不重新判定资格。
    private void retryLeftovers() {
        List<CheckinRewardCandidate> leftovers = new ArrayList<>(rewardPort.byStatus("pending"));
        rewardPort.byStatus("failed").stream()
                .filter(c -> c.attempts() < MAX_TRANSPORT_RETRIES)
                .forEach(leftovers::add);
        if (leftovers.isEmpty()) {
            return;
        }
        log.info("补给资格结算——重试遗留 {} 行", leftovers.size());
        sendByTierAndNotes(leftovers);
    }

    /// 扫描本月候选(满勤∧当月新增充值达标)、占位、预算闸门、发放。
    private void claimAndSendNewCandidates(YearMonth lastMonth) {
        LocalDate monthStart = lastMonth.atDay(1);
        LocalDate monthEnd = lastMonth.atEndOfMonth();
        LocalDate windowStart = monthStart.isAfter(props.launchDate()) ? monthStart : props.launchDate();
        if (windowStart.isAfter(monthEnd)) {
            return; // 上线日晚于上月月末:上月尚未有签到功能,无候选
        }
        long expectedDays = ChronoUnit.DAYS.between(windowStart, monthEnd) + 1;

        List<Long> fullAttendance = checkinPort.fullAttendanceUserIds(windowStart, monthEnd, expectedDays);
        if (fullAttendance.isEmpty()) {
            return;
        }
        Instant monthStartInstant = monthStart.atStartOfDay(ZONE).toInstant();
        Instant monthEndExclusiveInstant = monthStart.plusMonths(1).atStartOfDay(ZONE).toInstant();
        Map<Long, BigDecimal> recharges =
                rechargePort.monthlyRecharges(fullAttendance, monthStartInstant, monthEndExclusiveInstant);

        String notes = "checkin-reward-" + lastMonth.format(NOTES_MONTH);
        List<Long> sortedCandidates = fullAttendance.stream().sorted().toList(); // 稳定顺序,预算分配不依赖跑批顺序
        BigDecimal budgetLeft = settlementProps.monthlyBudgetCap();
        List<CheckinRewardCandidate> toSend = new ArrayList<>();

        for (long userId : sortedCandidates) {
            BigDecimal recharge = recharges.getOrDefault(userId, BigDecimal.ZERO);
            String tier = tierProps.tierFor(recharge).orElse(null);
            if (tier == null) {
                continue;
            }
            CheckinTierProperties.TierInfo info = tierProps.tiers().stream()
                    .filter(t -> t.tier().equals(tier)).findFirst().orElseThrow();
            var claimed = rewardPort.claim(userId, monthStart, tier, info.groupId(), notes);
            if (claimed.isEmpty()) {
                continue; // 本月已占位过(重跑/并发),交由 retryLeftovers 分支兜底
            }
            long grantId = claimed.get();
            boolean overBudget = info.costCny().compareTo(budgetLeft) > 0
                    || info.costCny().compareTo(settlementProps.perUserMonthlyCap()) > 0;
            if (overBudget) {
                rewardPort.markDeferred(grantId);
                log.warn("补给资格结算——预算硬顶已满,user={} tier={} 转 deferred", userId, tier);
                continue;
            }
            budgetLeft = budgetLeft.subtract(info.costCny());
            toSend.add(new CheckinRewardCandidate(grantId, userId, monthStart, tier, info.groupId(), notes, 0));
        }
        if (!toSend.isEmpty()) {
            sendByTierAndNotes(toSend);
        }
    }

    /// 按(tier,notes)分组调用 bulk-assign——不能只按 tier 分组:遗留重试可能同时混有不同月份
    /// (不同 notes)的同档候选,notes 不同绝不能塞进同一次 bulk-assign 调用。
    private void sendByTierAndNotes(List<CheckinRewardCandidate> candidates) {
        Map<String, List<CheckinRewardCandidate>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(c -> c.tier() + "|" + c.notes()));
        for (List<CheckinRewardCandidate> batch : grouped.values()) {
            String tier = batch.get(0).tier();
            long groupId = batch.get(0).groupId();
            String notes = batch.get(0).notes();
            int validityDays = tierProps.tiers().stream()
                    .filter(t -> t.tier().equals(tier)).findFirst().orElseThrow().validityDays();
            List<Long> userIds = batch.stream().map(CheckinRewardCandidate::userId).toList();

            SubscriptionGrantOutcome outcome = callBulkAssignWithRetry(userIds, groupId, validityDays, notes, batch);
            if (outcome == null) {
                continue; // 三次传输层重试均失败,已在 callBulkAssignWithRetry 内标记 failed + 告警
            }
            for (CheckinRewardCandidate c : batch) {
                String status = outcome.statuses().get(c.userId());
                if ("created".equals(status) || "reused".equals(status)) {
                    rewardPort.markSuccess(c.grantId());
                } else {
                    rewardPort.markFailed(c.grantId(), status == null ? "no status in response" : status);
                    log.error("补给资格发放失败 user={} tier={} status={}", c.userId(), c.tier(), status);
                }
            }
        }
    }

    /// 整批调用重试 ≤3 次(仅针对传输层异常,如网络/超时);三次均失败则把整批标记 failed 并
    /// 告警一次(spec §6"主动通知"),返回 null 交调用方跳过本批。
    private SubscriptionGrantOutcome callBulkAssignWithRetry(List<Long> userIds, long groupId, int validityDays,
            String notes, List<CheckinRewardCandidate> batch) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_TRANSPORT_RETRIES; attempt++) {
            try {
                return grantPort.bulkGrant(userIds, groupId, validityDays, notes);
            } catch (Exception e) {
                lastError = e;
                log.warn("bulk-assign 传输层调用失败,第 {} 次,group={}", attempt, groupId, e);
            }
        }
        for (CheckinRewardCandidate c : batch) {
            rewardPort.markFailed(c.grantId(), "transport failure after " + MAX_TRANSPORT_RETRIES + " retries");
        }
        log.error("补给资格发放——整批传输层调用连续 {} 次失败,已转人工告警 group={} users={}",
                MAX_TRANSPORT_RETRIES, groupId, userIds, lastError);
        return null;
    }
}
