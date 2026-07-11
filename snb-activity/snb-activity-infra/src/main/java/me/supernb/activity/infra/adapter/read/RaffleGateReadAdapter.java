package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.port.read.RaffleGateReadPort;
import me.supernb.sub2api.raffle.RaffleGateReadModel;
import org.springframework.stereotype.Component;

/// RaffleGateReadPort 实现:零逻辑薄委托 snb-sub2api 的门槛读模型(枚举转字符串在此收敛)。
@Component
public class RaffleGateReadAdapter implements RaffleGateReadPort {

    private final RaffleGateReadModel readModel;

    /// 构造:注入 starter 装配的门槛读模型。
    public RaffleGateReadAdapter(RaffleGateReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public BigDecimal gateValue(long userId, GateType type, Instant from, Instant to) {
        return readModel.gateValue(userId, type.name(), from, to);
    }

    @Override
    public Map<Long, BigDecimal> gateValues(Collection<Long> userIds, GateType type, Instant from, Instant to) {
        return readModel.gateValues(userIds, type.name(), from, to);
    }

    @Override
    public Map<Long, Instant> registeredAts(Collection<Long> userIds) {
        return readModel.registeredAts(userIds);
    }

    @Override
    public Map<Long, String> displayNames(Collection<Long> userIds) {
        return readModel.displayNamesByIds(userIds);
    }
}
