package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import me.supernb.activity.domain.port.read.AchievementRechargeReadPort;
import me.supernb.sub2api.gate.GateReadModel;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.springframework.stereotype.Component;

/// AchievementRechargeReadPort 实现:全量充值薄委托既有 GateReadModel(gate 闸机已用的同一
/// 只读能力,已含防双算口径);连续 3 月薄委托既有 RechargeReadModel 窗口口径三连查,
/// 任一窗口 ≤0 即短路返回 false。
@Component
public class AchievementRechargeReadAdapter implements AchievementRechargeReadPort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final GateReadModel gateReadModel;
    private final RechargeReadModel rechargeReadModel;

    /// 构造:注入两个既有 sub2api 只读读模型(均已由 gate/campaign 场景激活,零新增装配条件)。
    public AchievementRechargeReadAdapter(GateReadModel gateReadModel, RechargeReadModel rechargeReadModel) {
        this.gateReadModel = gateReadModel;
        this.rechargeReadModel = rechargeReadModel;
    }

    @Override
    public BigDecimal totalRecharged(long userId) {
        return gateReadModel.totalRecharged(userId);
    }

    @Override
    public boolean hasThreeConsecutiveMonthsOfRecharge(long userId) {
        LocalDate today = LocalDate.now(ZONE);
        for (int i = 0; i < 3; i++) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            Instant start = monthStart.atStartOfDay(ZONE).toInstant();
            Instant end = monthStart.plusMonths(1).atStartOfDay(ZONE).toInstant();
            BigDecimal amount = rechargeReadModel.totalRecharge(userId, start, end);
            if (amount.signum() <= 0) {
                return false;
            }
        }
        return true;
    }
}
