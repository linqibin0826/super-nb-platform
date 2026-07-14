package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import me.supernb.activity.domain.port.read.AchievementRechargeReadPort;
import me.supernb.sub2api.raffle.RaffleGateReadModel;
import org.springframework.stereotype.Component;

/// AchievementRechargeReadPort 实现:全量充值与连续 3 月均薄委托既有 RaffleGateReadModel 的
/// RECHARGE 全口径(payment_orders UNION 已核销 redeem_codes,剔 ZPay 镜像码防双算,2026-07-13
/// 站长拍板;与 A-7 CheckinRechargeReadPort 同款读模型,补给资格/充值成就口径统一——此前误接
/// GateReadModel/RechargeReadModel 只含 payment_orders,漏计闲鱼历史充值,已改正)。
/// 全量充值窗口=[Instant.EPOCH, now)(EPOCH 为本仓"全历史起点"既定哨兵,见 BoardPeriods.ALL);
/// 连续 3 月按月窗口三连查,任一窗口 ≤0 即短路返回 false。gateType 传字面量 "RECHARGE"——
/// 不导入 raffle 子域自己的 GateType 枚举,achievement 只借用这个只读能力,不感知 raffle 的
/// 领域类型(同 CheckinRechargeReadAdapter 惯例)。
@Component
public class AchievementRechargeReadAdapter implements AchievementRechargeReadPort {

    private static final String RECHARGE = "RECHARGE";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final RaffleGateReadModel raffleGateReadModel;

    /// 构造:注入既有 sub2api 只读读模型(已由 raffle 场景激活,零新增装配条件)。
    public AchievementRechargeReadAdapter(RaffleGateReadModel raffleGateReadModel) {
        this.raffleGateReadModel = raffleGateReadModel;
    }

    @Override
    public BigDecimal totalRecharged(long userId) {
        return raffleGateReadModel.gateValue(userId, RECHARGE, Instant.EPOCH, Instant.now());
    }

    @Override
    public boolean hasThreeConsecutiveMonthsOfRecharge(long userId) {
        LocalDate today = LocalDate.now(ZONE);
        for (int i = 0; i < 3; i++) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            Instant start = monthStart.atStartOfDay(ZONE).toInstant();
            Instant end = monthStart.plusMonths(1).atStartOfDay(ZONE).toInstant();
            BigDecimal amount = raffleGateReadModel.gateValue(userId, RECHARGE, start, end);
            if (amount.signum() <= 0) {
                return false;
            }
        }
        return true;
    }
}
