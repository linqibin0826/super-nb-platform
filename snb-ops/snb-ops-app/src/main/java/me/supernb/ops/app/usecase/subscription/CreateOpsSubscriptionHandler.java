package me.supernb.ops.app.usecase.subscription;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.ops.app.usecase.subscription.command.CreateOpsSubscriptionCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import org.springframework.stereotype.Service;

/// 建订阅用例:账号必须存在;数据过 SubscriptionValidator;同邮箱同服务撞唯一约束由 infra 映射 409。
@Service
public class CreateOpsSubscriptionHandler implements CommandHandler<CreateOpsSubscriptionCommand, String> {

    private final OpsAccountRepository accounts;
    private final OpsSubscriptionRepository subscriptions;

    /// 构造:注入两仓储端口。
    public CreateOpsSubscriptionHandler(OpsAccountRepository accounts, OpsSubscriptionRepository subscriptions) {
        this.accounts = accounts;
        this.subscriptions = subscriptions;
    }

    @Override
    public String handle(CreateOpsSubscriptionCommand cmd) {
        accounts.find(cmd.data().accountId())
                .orElseThrow(() -> OpsException.accountNotFound(cmd.data().accountId()));
        return String.valueOf(subscriptions.create(SubscriptionValidator.validate(cmd.data())));
    }
}
