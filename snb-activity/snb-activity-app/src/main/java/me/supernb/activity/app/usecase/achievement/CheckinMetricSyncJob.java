package me.supernb.activity.app.usecase.achievement;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.CheckinMetricSignalPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 签到相关成就 metric 生产者。日频(00:20)写累计/零点/诈尸三个即时指标;月频(次月 1 日
/// 01:10,比 Plan A 结算 job 晚 5 分钟错峰)写满勤累加计数与上线首月专属的两个绝版旗标。
/// `scanEnabled` 复用 Plan A 的 `CheckinSettlementProperties`(同一个 CHECKIN_SCAN_ENABLED
/// 开关,main spec 明文规定"Kill switch 关闭即停止全部成就判定")。
@Slf4j
@Service
public class CheckinMetricSyncJob {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final CheckinPort checkinPort;
    private final CheckinMetricSignalPort signalPort;
    private final UserMetricPort metricPort;
    private final CheckinProperties checkinProperties;
    private final CheckinSettlementProperties settlementProperties;

    /// 构造:注入 Plan A 的签到端口/配置与本计划的批量信号端口/指标底座。
    public CheckinMetricSyncJob(CheckinPort checkinPort, CheckinMetricSignalPort signalPort,
            UserMetricPort metricPort, CheckinProperties checkinProperties,
            CheckinSettlementProperties settlementProperties) {
        this.checkinPort = checkinPort;
        this.signalPort = signalPort;
        this.metricPort = metricPort;
        this.checkinProperties = checkinProperties;
        this.settlementProperties = settlementProperties;
    }

    /// 每日 00:20(Asia/Shanghai)入口。
    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Shanghai")
    public void syncDaily() {
        syncDailyAt(LocalDate.now(ZONE));
    }

    /// 测试友好重载:显式传入"今天"。
    void syncDailyAt(LocalDate today) {
        if (!settlementProperties.scanEnabled()) {
            log.info("签到 metric 日频同步已跳过:scanEnabled=false");
            return;
        }
        List<Long> todayCheckers = signalPort.usersCheckedInOn(today);
        if (todayCheckers.isEmpty()) {
            return;
        }
        Set<Long> midnightUsers = new HashSet<>(signalPort.usersCheckedInAtMidnightOn(today));
        for (long userId : todayCheckers) {
            try {
                metricPort.upsert(userId, "checkin_total_count", checkinPort.totalCheckins(userId));
                if (midnightUsers.contains(userId)) {
                    metricPort.upsert(userId, "checkin_midnight_flag", 1);
                }
                if (signalPort.hasGhostReturnAsOf(userId, today)) {
                    metricPort.upsert(userId, "checkin_ghost_return_flag", 1);
                }
            } catch (Exception e) {
                log.error("签到 metric 日频同步失败 user={}", userId, e);
            }
        }
    }

    /// 次月 1 日 01:10(Asia/Shanghai)入口:结算"上个月"。
    @Scheduled(cron = "0 10 1 1 * *", zone = "Asia/Shanghai")
    public void syncMonthly() {
        if (!settlementProperties.scanEnabled()) {
            log.info("签到 metric 月频同步已跳过:scanEnabled=false");
            return;
        }
        syncMonthlyFor(YearMonth.now(ZONE).minusMonths(1));
    }

    /// 测试友好重载:显式传入"上个月"。
    void syncMonthlyFor(YearMonth lastMonth) {
        LocalDate monthStart = lastMonth.atDay(1);
        LocalDate monthEnd = lastMonth.atEndOfMonth();
        long expectedDays = ChronoUnit.DAYS.between(monthStart, monthEnd) + 1;

        for (long userId : checkinPort.fullAttendanceUserIds(monthStart, monthEnd, expectedDays)) {
            double current = metricPort.value(userId, "checkin_fullmonth_count").orElse(0.0);
            metricPort.upsert(userId, "checkin_fullmonth_count", current + 1);
        }

        YearMonth launchMonth = YearMonth.from(checkinProperties.launchDate());
        if (!lastMonth.equals(launchMonth)) {
            return; // 绝版旗标只在上线首月那一次结算里计算,之后这段代码路径永不再触发
        }
        LocalDate launchDate = checkinProperties.launchDate();
        for (long userId : signalPort.usersCheckedInBetween(launchDate, monthEnd)) {
            metricPort.upsert(userId, "checkin_founding_month_flag", 1);
        }
        long expectedFoundingDays = ChronoUnit.DAYS.between(launchDate, monthEnd) + 1;
        for (long userId : checkinPort.fullAttendanceUserIds(launchDate, monthEnd, expectedFoundingDays)) {
            metricPort.upsert(userId, "checkin_founding_fullmonth_flag", 1);
        }
    }
}
