package me.supernb.ops.app.usecase.subscription;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.ops.app.usecase.subscription.command.UpdateOpsSubscriptionCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import org.springframework.stereotype.Service;

/// 改订阅用例(全量覆盖):校验同建;仓储未命中 404。
@Service
public class UpdateOpsSubscriptionHandler implements CommandHandler<UpdateOpsSubscriptionCommand, Void> {

    private final OpsSubscriptionRepository subscriptions;

    /// 构造:注入订阅仓储端口。
    public UpdateOpsSubscriptionHandler(OpsSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public Void handle(UpdateOpsSubscriptionCommand cmd) {
        if (!subscriptions.update(cmd.id(), SubscriptionValidator.validate(cmd.data()))) {
            throw OpsException.subscriptionNotFound(cmd.id());
        }
        return null;
    }
}
