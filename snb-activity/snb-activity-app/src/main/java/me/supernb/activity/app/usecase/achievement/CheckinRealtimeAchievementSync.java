package me.supernb.activity.app.usecase.achievement;

import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.command.UserCheckedInEvent;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/// 打卡实时成就同步:监听 UserCheckedInEvent(同步,打卡响应拼装前跑完),即时补写该用户的
/// checkin 指标并判定解锁,让成就打卡当场亮。总闸 scanEnabled 关闭即短路(spec 明文:关闭停止
/// 全部成就判定)。异常一律吞掉——打卡已成功提交,成就同步失败由日频同步(次日 00:20)+每小时
/// 判定引擎兜底,绝不因成就侧问题让打卡接口报错。
@Slf4j
@Service
public class CheckinRealtimeAchievementSync {

    private final CheckinMetricSyncJob metricSyncJob;
    private final AchievementJudgeEngine judgeEngine;
    private final CheckinSettlementProperties settlementProperties;

    public CheckinRealtimeAchievementSync(CheckinMetricSyncJob metricSyncJob,
            AchievementJudgeEngine judgeEngine, CheckinSettlementProperties settlementProperties) {
        this.metricSyncJob = metricSyncJob;
        this.judgeEngine = judgeEngine;
        this.settlementProperties = settlementProperties;
    }

    @EventListener
    public void onUserCheckedIn(UserCheckedInEvent event) {
        if (!settlementProperties.scanEnabled()) {
            return;
        }
        try {
            metricSyncJob.syncUserForDay(event.userId(), event.day());
            judgeEngine.judgeUser(event.userId(), "checkin_realtime");
        } catch (Exception e) {
            log.warn("打卡实时成就同步失败(打卡已成功,兜底=日频同步+每小时引擎) user={}", event.userId(), e);
        }
    }
}
