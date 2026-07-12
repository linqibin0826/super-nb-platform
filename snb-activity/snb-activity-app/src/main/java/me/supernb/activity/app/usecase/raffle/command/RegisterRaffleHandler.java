package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.exception.RaffleNotEligibleException;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.exception.RaffleNotOpenException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RaffleEntryTicket;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleEntryPort;
import me.supernb.activity.domain.port.read.RaffleGateReadPort;
import org.springframework.stereotype.Service;

/// 申请列席:期存在、在报名窗口、账龄够(若配置)、门槛达标,全过才委托 enter(幂等)。
/// 资质不足的 409 文案带「还需 ¥XX」明细;取号原子性在 infra(advisory lock),
/// Handler 无事务注解(事务边界收在 infra 的仓库纪律)。
@Service
public class RegisterRaffleHandler implements CommandHandler<RegisterRaffleCommand, RaffleEntryTicket> {

    private final RaffleCampaignPort campaignPort;
    private final RaffleEntryPort entryPort;
    private final RaffleGateReadPort gatePort;

    /// 构造:注入期/报名/门槛端口。
    public RegisterRaffleHandler(RaffleCampaignPort campaignPort, RaffleEntryPort entryPort,
            RaffleGateReadPort gatePort) {
        this.campaignPort = campaignPort;
        this.entryPort = entryPort;
        this.gatePort = gatePort;
    }

    @Override
    public RaffleEntryTicket handle(RegisterRaffleCommand command) {
        RaffleCampaign c = campaignPort.byId(command.campaignId()).orElseThrow(RaffleNotFoundException::new);
        Instant now = Instant.now();
        if (!c.openForEntry(now)) {
            throw new RaffleNotOpenException();
        }
        if (c.minAccountAgeDays() != null) {
            Instant registered = gatePort.registeredAts(List.of(command.userId())).get(command.userId());
            Instant cutoff = now.minus(Duration.ofDays(c.minAccountAgeDays()));
            if (registered == null || registered.isAfter(cutoff)) {
                throw new RaffleNotEligibleException(
                        "列席资质审核未通过:账号注册须满 " + c.minAccountAgeDays() + " 天");
            }
        }
        BigDecimal value = gatePort.gateValue(command.userId(), c.gateType(), c.gateFrom(), now);
        if (value.compareTo(c.gateAmount()) < 0) {
            BigDecimal shortfall = c.gateAmount().subtract(value).stripTrailingZeros();
            throw new RaffleNotEligibleException("列席资质审核未通过:还需"
                    + (c.gateType() == GateType.RECHARGE ? "充值" : "消费")
                    + " ¥" + shortfall.toPlainString());
        }
        return entryPort.enter(c.id(), command.userId(), value, command.clientIp(), command.userAgent());
    }
}
