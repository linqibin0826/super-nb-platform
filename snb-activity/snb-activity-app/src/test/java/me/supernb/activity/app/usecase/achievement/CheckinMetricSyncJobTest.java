package me.supernb.activity.app.usecase.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.CheckinMetricSignalPort;
import org.junit.jupiter.api.Test;

/// 日频:开关关闭时短路;今日签到者的累计/零点/诈尸三指标各自正确写入。
class CheckinMetricSyncJobTest {

    private final CheckinPort checkinPort = mock(CheckinPort.class);
    private final CheckinMetricSignalPort signalPort = mock(CheckinMetricSignalPort.class);
    private final UserMetricPort metricPort = mock(UserMetricPort.class);
    private final CheckinProperties checkinProperties = new CheckinProperties("2020-01-01", 3);

    private CheckinMetricSyncJob job(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new CheckinMetricSyncJob(checkinPort, signalPort, metricPort, checkinProperties, settlementProperties);
    }

    @Test
    void syncDailySkipsWhenScanDisabled() {
        job(false).syncDaily();
        verify(signalPort, never()).usersCheckedInOn(any());
    }

    @Test
    void syncDailyWritesCumulativeAndFlagMetricsForTodayCheckers() {
        LocalDate today = LocalDate.of(2026, 7, 13);
        when(signalPort.usersCheckedInOn(today)).thenReturn(List.of(42L));
        when(signalPort.usersCheckedInAtMidnightOn(today)).thenReturn(List.of(42L));
        when(checkinPort.totalCheckins(42)).thenReturn(13);
        when(signalPort.hasGhostReturnAsOf(42, today)).thenReturn(false);

        job(true).syncDailyAt(today);

        verify(metricPort).upsert(42, "checkin_total_count", 13);
        verify(metricPort).upsert(42, "checkin_midnight_flag", 1);
    }

    @Test
    void syncMonthlyIncrementsFullMonthCountAndWritesFoundingFlagsDuringLaunchMonth() {
        java.time.YearMonth lastMonth = java.time.YearMonth.of(2026, 7);
        LocalDate monthStart = lastMonth.atDay(1);
        LocalDate monthEnd = lastMonth.atEndOfMonth();
        when(checkinPort.fullAttendanceUserIds(monthStart, monthEnd, 31)).thenReturn(List.of(42L));
        when(metricPort.value(42, "checkin_fullmonth_count")).thenReturn(Optional.of(2.0));
        CheckinProperties launchInJuly = new CheckinProperties("2026-07-01", 3);
        when(signalPort.usersCheckedInBetween(eq(LocalDate.of(2026, 7, 1)), eq(monthEnd)))
                .thenReturn(List.of(42L));
        when(checkinPort.fullAttendanceUserIds(LocalDate.of(2026, 7, 1), monthEnd, 31))
                .thenReturn(List.of(42L));

        CheckinSettlementProperties settlementProperties =
                new CheckinSettlementProperties(new BigDecimal("250"), new BigDecimal("10"), true, false);
        new CheckinMetricSyncJob(checkinPort, signalPort, metricPort, launchInJuly, settlementProperties)
                .syncMonthlyFor(lastMonth);

        verify(metricPort).upsert(42, "checkin_fullmonth_count", 3.0);
        verify(metricPort).upsert(42, "checkin_founding_month_flag", 1);
        verify(metricPort).upsert(42, "checkin_founding_fullmonth_flag", 1);
    }

    @Test
    void syncDailySettlesYesterdayNotToday() {
        // 生产接线 syncDaily() 在 00:20 跑,必须结算"刚过去的昨天"(那天已完整);不是"今天"
        // (00:20 时今天只有 0–20 分钟的零点打卡者)。回归 2026-07-15 白天打卡者永不解锁开机自检的日期 bug。
        LocalDate yesterday = LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusDays(1);
        when(signalPort.usersCheckedInOn(yesterday)).thenReturn(List.of(42L));
        when(signalPort.usersCheckedInAtMidnightOn(yesterday)).thenReturn(List.of());
        when(checkinPort.totalCheckins(42)).thenReturn(1);
        when(signalPort.hasGhostReturnAsOf(42, yesterday)).thenReturn(false);

        job(true).syncDaily();

        verify(signalPort).usersCheckedInOn(yesterday);
        verify(metricPort).upsert(42, "checkin_total_count", 1);
    }


    @Test
    void syncUserForDayWritesCumulativeAndFlagsForSingleUser() {
        LocalDate day = LocalDate.of(2026, 7, 15);
        when(checkinPort.totalCheckins(7)).thenReturn(1);
        when(signalPort.usersCheckedInAtMidnightOn(day)).thenReturn(List.of(7L));
        when(signalPort.hasGhostReturnAsOf(7, day)).thenReturn(false);

        job(true).syncUserForDay(7, day);

        verify(metricPort).upsert(7, "checkin_total_count", 1);
        verify(metricPort).upsert(7, "checkin_midnight_flag", 1);
    }

}
