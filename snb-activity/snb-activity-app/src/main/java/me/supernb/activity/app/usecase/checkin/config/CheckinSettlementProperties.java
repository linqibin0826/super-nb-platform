package me.supernb.activity.app.usecase.checkin.config;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 月度结算预算硬顶与功能开关(spec §5.3/§7.5/§7.7)。`scanEnabled` 关闭即停止全部签到相关
/// 批处理判定与发放(签到打卡本身不受影响);`tierRewardEnabled` 把高风险的补给资格链路与
/// 低风险的签到主线解耦(决策6)——两轮红队报告的高/中高严重度发现几乎全集中在补给资格这条链路。
/// 两者均默认 `false`(硬阻断,spec §6 核实清单①未过一律不得打开)。
@Component
public class CheckinSettlementProperties {

    private final BigDecimal monthlyBudgetCap;
    private final BigDecimal perUserMonthlyCap;
    private final boolean scanEnabled;
    private final boolean tierRewardEnabled;

    /// 构造:预算硬顶/保险丝/两个开关经配置注入。
    public CheckinSettlementProperties(
            @Value("${activity.checkin.monthly-budget-cap:250}") BigDecimal monthlyBudgetCap,
            @Value("${activity.checkin.per-user-monthly-cap:10}") BigDecimal perUserMonthlyCap,
            @Value("${activity.checkin.scan-enabled:false}") boolean scanEnabled,
            @Value("${activity.checkin.tier-reward-enabled:false}") boolean tierRewardEnabled) {
        this.monthlyBudgetCap = monthlyBudgetCap;
        this.perUserMonthlyCap = perUserMonthlyCap;
        this.scanEnabled = scanEnabled;
        this.tierRewardEnabled = tierRewardEnabled;
    }

    /// 系统硬顶(元/月),不因估算下修而调低(spec §5.3)。
    public BigDecimal monthlyBudgetCap() {
        return monthlyBudgetCap;
    }

    /// 个人保险丝(元/月),单档最高成本(C 档 ¥4.4)远低于此值,属防御性冗余闸门。
    public BigDecimal perUserMonthlyCap() {
        return perUserMonthlyCap;
    }

    /// 批处理总闸:关闭即全部签到相关批处理(含未来 Plan B 的成就判定)停止运行。
    public boolean scanEnabled() {
        return scanEnabled;
    }

    /// 补给资格独立开关:关闭时签到/里程碑/成就主线正常运行,只是不触发发放。
    public boolean tierRewardEnabled() {
        return tierRewardEnabled;
    }
}
