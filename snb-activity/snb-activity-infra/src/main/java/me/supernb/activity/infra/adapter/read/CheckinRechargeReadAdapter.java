package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import me.supernb.sub2api.raffle.RaffleGateReadModel;
import org.springframework.stereotype.Component;

/// CheckinRechargeReadPort 实现:薄委托既有 RaffleGateReadModel 的 RECHARGE 窗口口径
/// (v0.1.10 已修复"剔除 ZPay 同码镜像防双算",不重新实现这段易错 SQL)。gateType 传字面量
/// "RECHARGE"——不导入 raffle 子域自己的 GateType 枚举,checkin 只借用这个只读能力,
/// 不感知 raffle 的领域类型。
@Component
public class CheckinRechargeReadAdapter implements CheckinRechargeReadPort {

    private static final String RECHARGE = "RECHARGE";

    private final RaffleGateReadModel readModel;

    /// 构造:注入 starter 装配的门槛读模型(已由 raffle 场景激活,零新增装配条件)。
    public CheckinRechargeReadAdapter(RaffleGateReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public BigDecimal monthlyRecharge(long userId, Instant monthStart, Instant monthEndExclusive) {
        return readModel.gateValue(userId, RECHARGE, monthStart, monthEndExclusive);
    }

    @Override
    public Map<Long, BigDecimal> monthlyRecharges(Collection<Long> userIds, Instant monthStart,
            Instant monthEndExclusive) {
        return readModel.gateValues(userIds, RECHARGE, monthStart, monthEndExclusive);
    }
}
