package me.supernb.activity.app.usecase.achievement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 成就系统专属配置。⚠️ 与 Plan A 的 `CheckinSettlementProperties.scanEnabled()` 是两个独立开关:
/// 那个是"批处理总闸"(关了全部签到+成就批处理都停),这个是"首刷一次性开关"(只影响首刷 job
/// 是否曾经跑过一次存量回填,跑过一次后再切它也不会重跑——门控在水位线是否存在,不在本开关本身)。
///
/// 🪦 `enabled`(默认 **false**)——成就系统整体停用总闸(2026-07-28 站长拍板"暂时下线"):
/// 全部 11 个成就 bean(10 个调度/判定 job + 打卡实时同步)挂
/// `@ConditionalOnProperty(activity.achievement.enabled=true)` 整体不装配,墙/已读两个端点 404。
/// **暂时下线≠退役**:代码/测试/数据(achievement_unlock、nb_ledger 照写)全保留,boot 层测试显式
/// 开 true 继续验证全链,重开=生产 compose 加一行 env 映射 true + registry.json 摘 hidden +
/// 恢复签到页入口(见 runbook 32「成就系统暂时下线」节;重开前先补 RechargeAchievementJudgeJob
/// 类目过滤串的新旧双认——全局 review 2026-07-28 发现 #10,下线期间挂账)。
@Component
public class AchievementProperties {

    private final boolean retroactiveBackfillEnabled;
    private final boolean enabled;

    public AchievementProperties(
            @Value("${activity.achievement.retroactive-backfill-enabled:false}") boolean retroactiveBackfillEnabled,
            @Value("${activity.achievement.enabled:false}") boolean enabled) {
        this.retroactiveBackfillEnabled = retroactiveBackfillEnabled;
        this.enabled = enabled;
    }

    /// 首刷一次性开关。
    public boolean retroactiveBackfillEnabled() {
        return retroactiveBackfillEnabled;
    }

    /// 成就系统整体开关(读端点用;调度/判定 bean 由同名属性的 @ConditionalOnProperty 控制)。
    public boolean enabled() {
        return enabled;
    }
}
