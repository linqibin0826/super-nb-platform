package me.supernb.activity.app.usecase.achievement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 成就系统专属配置。⚠️ 与 Plan A 的 `CheckinSettlementProperties.scanEnabled()` 是两个独立开关:
/// 那个是"批处理总闸"(关了全部签到+成就批处理都停),这个是"首刷一次性开关"(只影响首刷 job
/// 是否曾经跑过一次存量回填,跑过一次后再切它也不会重跑——门控在水位线是否存在,不在本开关本身)。
@Component
public class AchievementProperties {

    private final boolean retroactiveBackfillEnabled;

    public AchievementProperties(
            @Value("${activity.achievement.retroactive-backfill-enabled:false}") boolean retroactiveBackfillEnabled) {
        this.retroactiveBackfillEnabled = retroactiveBackfillEnabled;
    }

    /// 首刷一次性开关。
    public boolean retroactiveBackfillEnabled() {
        return retroactiveBackfillEnabled;
    }
}
