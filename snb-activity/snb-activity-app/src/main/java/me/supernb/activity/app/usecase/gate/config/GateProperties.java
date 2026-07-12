package me.supernb.activity.app.usecase.gate.config;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 金票闸机配置(spec gate §1/§4):门槛与概率,值来自 `activity.gate.*`(application.yml / env)。
/// app 模块只依赖 spring-context,照 ReferralProperties 用 @Value 构造注入。
/// win-rate ≤ 0 = 闸机休眠(handler 短路,连资格都不查)。
@Component
public class GateProperties {

    private final BigDecimal thresholdCny;
    private final double winRate;

    public GateProperties(
            @Value("${activity.gate.threshold-cny:30}") BigDecimal thresholdCny,
            @Value("${activity.gate.win-rate:0.02}") double winRate) {
        this.thresholdCny = thresholdCny;
        this.winRate = winRate;
    }

    public BigDecimal thresholdCny() {
        return thresholdCny;
    }

    public double winRate() {
        return winRate;
    }
}
