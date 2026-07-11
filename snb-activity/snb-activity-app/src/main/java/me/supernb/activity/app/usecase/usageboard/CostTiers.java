package me.supernb.activity.app.usecase.usageboard;

import me.supernb.activity.domain.model.read.usage.BoardView;

/// 花费档位工具类(纯函数,无框架依赖)。
///
/// 档位边界左闭:T_10K≥10000,T_5K≥5000,T_1K≥1000,T_500≥500,T_100≥100,T_10≥10,T_0&lt;10
/// (出处 spec §5/§7)。
public final class CostTiers {

    // 档位表,自高到低排列;tier()/nextTierGap() 共用同一份口径,避免两处维护边界值。
    private static final Object[][] TIERS = {
            {"T_10K", 10000.0},
            {"T_5K", 5000.0},
            {"T_1K", 1000.0},
            {"T_500", 500.0},
            {"T_100", 100.0},
            {"T_10", 10.0},
            {"T_0", 0.0},
    };

    /// 工具类,禁止实例化。
    private CostTiers() {
    }

    /// 按花费金额算出所在档位码,自最高档起左闭匹配(出处 spec §5/§7)。
    ///
    /// @param cost 花费金额(元)
    /// @return 档位码,T_10K/T_5K/T_1K/T_500/T_100/T_10/T_0 之一
    public static String tier(double cost) {
        return (String) TIERS[tierIndex(cost)][0];
    }

    /// 算出距下一(更高)档位的差距,已是最高档 T_10K 时返回 null(出处 spec §5/§7)。
    ///
    /// @param cost 花费金额(元)
    /// @return 下一档位码 + 差额;已是最高档时为 null
    public static BoardView.TierGap nextTierGap(double cost) {
        int idx = tierIndex(cost);
        if (idx == 0) {
            return null; // 已是最高档
        }
        Object[] next = TIERS[idx - 1];
        return new BoardView.TierGap((String) next[0], (double) next[1] - cost);
    }

    private static int tierIndex(double cost) {
        for (int i = 0; i < TIERS.length; i++) {
            if (cost >= (double) TIERS[i][1]) {
                return i;
            }
        }
        return TIERS.length - 1; // 兜底:负数按最低档 T_0 处理
    }
}
