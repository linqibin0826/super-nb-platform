package me.supernb.activity.app.usecase.checkin.config;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 签到准入闸配置(spec §12,2026-07-31 站长拍板):近 window-days 天真实充值 ≥ min-cny
/// 才能签到——把签到从「白送」改成「付费客户回馈」。
///
/// enabled 默认 false(fail-open 保持旧行为):不配 env 就人人可签,与 CHECKIN_SCAN_ENABLED
/// 同款纪律(⚠️ compose 不加 environment 映射 = yml 默认值静默生效,GATE_ 老教训)。
/// 金额字符串注入转 BigDecimal,照 [CheckinBalanceProperties] 惯例。
/// ⚠️ 命名刻意用 entry-gate/CHECKIN_ENTRY_ 前缀,别与金票闸机的 activity.gate.*(GATE_)混淆。
@Component
public class CheckinEntryGateProperties {

    private final boolean enabled;
    private final int windowDays;
    private final BigDecimal minCny;

    /// 构造:@Value 注入(app 模块只依赖 spring-context,照 CheckinProperties 惯例)。
    public CheckinEntryGateProperties(
            @Value("${activity.checkin.entry-gate.enabled:false}") boolean enabled,
            @Value("${activity.checkin.entry-gate.window-days:30}") int windowDays,
            @Value("${activity.checkin.entry-gate.min-cny:30}") String minCny) {
        this.enabled = enabled;
        this.windowDays = windowDays;
        this.minCny = new BigDecimal(minCny);
    }

    /// 准入闸总闸;false = 不设门槛,人人可签(旧行为)。
    public boolean enabled() {
        return enabled;
    }

    /// 滚动窗口天数(含今天的自然日数)。
    public int windowDays() {
        return windowDays;
    }

    /// 窗口内真实充值门槛(元)。
    public BigDecimal minCny() {
        return minCny;
    }
}
