package me.supernb.activity.app.usecase.checkin.config;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 每日返网费配置(spec 2026-07-31-checkin-daily-reward §6)。
///
/// 金额一律以**字符串**注入再转 BigDecimal——@Value 直绑 double 会丢精度,而这里的数
/// 直接决定给用户发多少钱。enabled 默认 false(fail-closed):不配 env 就只发 NB 不发钱,
/// 与 CHECKIN_SCAN_ENABLED 同款纪律(⚠️ compose 不加 environment 映射 = yml 默认值静默生效)。
@Component
public class CheckinBalanceProperties {

    private final boolean enabled;
    private final BigDecimal perDayCny;
    private final BigDecimal thresholdCny;
    private final BigDecimal monthlyCapCny;
    private final int stepDays;

    /// 构造:@Value 注入(app 模块只依赖 spring-context,照 CheckinProperties 惯例)。
    public CheckinBalanceProperties(
            @Value("${activity.checkin.balance.enabled:false}") boolean enabled,
            @Value("${activity.checkin.balance.per-day-cny:0.1}") String perDayCny,
            @Value("${activity.checkin.balance.threshold-cny:30}") String thresholdCny,
            @Value("${activity.checkin.balance.monthly-cap-cny:3000}") String monthlyCapCny,
            @Value("${activity.checkin.balance.step-days:1}") int stepDays) {
        this.enabled = enabled;
        this.perDayCny = new BigDecimal(perDayCny);
        this.thresholdCny = new BigDecimal(thresholdCny);
        this.monthlyCapCny = new BigDecimal(monthlyCapCny);
        this.stepDays = stepDays;
    }

    /// 返网费总闸;false = 只发 NB 不发钱。
    public boolean enabled() {
        return enabled;
    }

    /// 每连签一天的返网费单价(元)。
    public BigDecimal perDayCny() {
        return perDayCny;
    }

    /// 送余额的历史累计真实充值门槛(元)。
    public BigDecimal thresholdCny() {
        return thresholdCny;
    }

    /// 全站月度返网费预算硬顶(元);打满后只停余额、NB 照发。
    public BigDecimal monthlyCapCny() {
        return monthlyCapCny;
    }

    /// 几天升一档(spec §12,2026-07-31 两天一档拍板):第 N 天返 perDayCny × ⌈N/stepDays⌉。
    /// 1 = 原线性费率(默认,向后兼容)。
    public int stepDays() {
        return stepDays;
    }
}
