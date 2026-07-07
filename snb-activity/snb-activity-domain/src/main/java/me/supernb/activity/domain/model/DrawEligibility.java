package me.supernb.activity.domain.model;

import java.math.BigDecimal;

/// 抽奖资格领域规则(纯函数,无框架依赖)。
///
/// 口径同 activity-svc:每满 ¥100 得一次抽奖机会;应得次数 = floor(充值总额 / 100),
/// 剩余 = max(0, 应得 - 已抽)。充值倍率 1:1,金额单位为名义额度,不是按真实汇率折算
/// 出来的美元(见 ai-relay 红线)。
public final class DrawEligibility {

    /// 每次抽奖所需的充值门槛(元)。
    public static final BigDecimal DRAW_THRESHOLD = new BigDecimal("100");

    /// 工具类,禁止实例化。
    private DrawEligibility() {
    }

    /// 计算剩余可抽次数:充值总额换算出的应得次数减去已抽次数。
    ///
    /// @param totalRecharge 活动期内已完成的充值总额(元);null 或非正视为 0
    /// @param usedDraws     已抽次数
    /// @return 剩余可抽次数(不为负)
    public static int remainingDraws(BigDecimal totalRecharge, int usedDraws) {
        if (totalRecharge == null || totalRecharge.signum() <= 0) {
            return 0;
        }
        int earned = totalRecharge.divideToIntegralValue(DRAW_THRESHOLD).intValueExact();
        return Math.max(0, earned - usedDraws);
    }
}
