package me.supernb.activity.app.usecase.gate;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import me.supernb.activity.app.usecase.gate.command.PerformGateDrawCommand;
import me.supernb.activity.app.usecase.gate.config.GateProperties;
import me.supernb.activity.domain.exception.GateAlreadyAttemptedTodayException;
import me.supernb.activity.domain.port.gate.GatePort;
import me.supernb.activity.domain.port.read.GateRechargeReadPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 金票闸机抽签编排(spec gate §4):休眠短路 → 门槛(累计真实充值) → 服务端 RNG →
/// 委托 GatePort 事务体。并发双击撞唯一键 → 以 wantWin=false 换新事务降级重读当日结果
/// (第一事务连同已领的码整体回滚,池不漏)。事务边界收在 infra,本层无事务注解(家族约定)。
@Service
public class PerformGateDrawHandler implements CommandHandler<PerformGateDrawCommand, GateDrawResult> {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final GateProperties props;
    private final GateRechargeReadPort recharge;
    private final GatePort gatePort;
    private final DoubleSupplier rng;

    /// 构造(Spring 用):RNG 缺省 ThreadLocalRandom。
    @Autowired
    public PerformGateDrawHandler(GateProperties props, GateRechargeReadPort recharge, GatePort gatePort) {
        this(props, recharge, gatePort, () -> ThreadLocalRandom.current().nextDouble());
    }

    /// 构造(测试用):RNG 可注入保判定分支确定性。
    PerformGateDrawHandler(GateProperties props, GateRechargeReadPort recharge, GatePort gatePort,
                           DoubleSupplier rng) {
        this.props = props;
        this.recharge = recharge;
        this.gatePort = gatePort;
        this.rng = rng;
    }

    @Override
    public GateDrawResult handle(PerformGateDrawCommand command) {
        if (props.winRate() <= 0) {
            return GateDrawResult.ineligible();
        }
        if (recharge.totalRecharged(command.userId()).compareTo(props.thresholdCny()) < 0) {
            return GateDrawResult.ineligible();
        }
        LocalDate day = LocalDate.now(ZONE);
        boolean want = rng.getAsDouble() < props.winRate();
        try {
            return GateDrawResult.of(gatePort.drawFor(command.userId(), day, want));
        } catch (GateAlreadyAttemptedTodayException race) {
            return GateDrawResult.of(gatePort.drawFor(command.userId(), day, false));
        }
    }
}
