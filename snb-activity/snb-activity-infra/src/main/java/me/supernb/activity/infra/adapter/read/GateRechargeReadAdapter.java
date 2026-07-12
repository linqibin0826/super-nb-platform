package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import me.supernb.activity.domain.port.read.GateRechargeReadPort;
import me.supernb.sub2api.gate.GateReadModel;
import org.springframework.stereotype.Component;

/// GateRechargeReadPort 实现:薄适配,委托 snb-sub2api 的 GateReadModel(累计真实充值)。
@Component
public class GateRechargeReadAdapter implements GateRechargeReadPort {

    private final GateReadModel readModel;

    /// 构造:注入 starter 提供的金票门槛读模型。
    public GateRechargeReadAdapter(GateReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public BigDecimal totalRecharged(long userId) {
        return readModel.totalRecharged(userId);
    }
}
