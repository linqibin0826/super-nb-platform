package me.supernb.activity.app.usecase.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import me.supernb.activity.app.usecase.checkin.command.UserCheckedInEvent;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import org.junit.jupiter.api.Test;

/// 打卡实时成就同步监听器:总闸关闭整体短路;开启时先补写指标再判定;判定抛异常被吞不外抛
/// (打卡已提交,绝不因成就侧牵连)。
class CheckinRealtimeAchievementSyncTest {

    private final CheckinMetricSyncJob metricSyncJob = mock(CheckinMetricSyncJob.class);
    private final AchievementJudgeEngine judgeEngine = mock(AchievementJudgeEngine.class);

    private CheckinRealtimeAchievementSync sync(boolean scanEnabled) {
        CheckinSettlementProperties props = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new CheckinRealtimeAchievementSync(metricSyncJob, judgeEngine, props);
    }

    @Test
    void skipsEntirelyWhenScanDisabled() {
        sync(false).onUserCheckedIn(new UserCheckedInEvent(42, LocalDate.of(2026, 7, 15)));
        verify(metricSyncJob, never()).syncUserForDay(anyLong(), any());
        verify(judgeEngine, never()).judgeUser(anyLong(), anyString());
    }

    @Test
    void syncsMetricThenJudgesWhenEnabled() {
        LocalDate day = LocalDate.of(2026, 7, 15);
        sync(true).onUserCheckedIn(new UserCheckedInEvent(42, day));
        verify(metricSyncJob).syncUserForDay(42, day);
        verify(judgeEngine).judgeUser(42, "checkin_realtime");
    }

    @Test
    void swallowsExceptionSoCheckinNeverFails() {
        LocalDate day = LocalDate.of(2026, 7, 15);
        doThrow(new RuntimeException("boom")).when(judgeEngine).judgeUser(anyLong(), anyString());
        sync(true).onUserCheckedIn(new UserCheckedInEvent(42, day)); // 不外抛即通过
        verify(judgeEngine).judgeUser(42, "checkin_realtime");
    }
}
