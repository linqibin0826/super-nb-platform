package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import me.supernb.activity.domain.port.read.UserBalanceReadPort;
import me.supernb.sub2api.account.UserBalanceReadModel;
import org.springframework.stereotype.Component;

/// UserBalanceReadPort 实现:薄适配,委托 snb-sub2api 的 UserBalanceReadModel(当前余额)。
@Component
public class UserBalanceReadAdapter implements UserBalanceReadPort {

    private final UserBalanceReadModel readModel;

    /// 构造:注入 starter 提供的用户余额读模型。
    public UserBalanceReadAdapter(UserBalanceReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public BigDecimal balance(long userId) {
        return readModel.balance(userId);
    }
}
