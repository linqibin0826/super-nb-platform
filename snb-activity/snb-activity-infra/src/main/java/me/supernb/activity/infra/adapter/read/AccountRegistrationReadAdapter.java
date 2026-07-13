package me.supernb.activity.infra.adapter.read;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import me.supernb.sub2api.raffle.RaffleGateReadModel;
import org.springframework.stereotype.Component;

/// AccountRegistrationReadPort 实现:零逻辑薄委托既有 RaffleGateReadModel.registeredAts
/// (raffle 账龄门槛已用的同一只读能力;签到不新增 sub2api ACL 面)。
@Component
public class AccountRegistrationReadAdapter implements AccountRegistrationReadPort {

    private final RaffleGateReadModel readModel;

    /// 构造:注入 starter 装配的门槛读模型(已由 raffle 场景激活,零新增装配条件)。
    public AccountRegistrationReadAdapter(RaffleGateReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public Optional<Instant> registeredAt(long userId) {
        return Optional.ofNullable(readModel.registeredAts(List.of(userId)).get(userId));
    }
}
