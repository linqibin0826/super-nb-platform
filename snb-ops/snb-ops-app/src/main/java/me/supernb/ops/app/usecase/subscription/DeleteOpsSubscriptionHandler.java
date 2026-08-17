package me.supernb.ops.app.usecase.subscription;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.ops.app.usecase.subscription.command.DeleteOpsSubscriptionCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import org.springframework.stereotype.Service;

/// 删订阅用例:删不到 404。
@Service
public class DeleteOpsSubscriptionHandler implements CommandHandler<DeleteOpsSubscriptionCommand, Void> {

    private final OpsSubscriptionRepository subscriptions;

    /// 构造:注入订阅仓储端口。
    public DeleteOpsSubscriptionHandler(OpsSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public Void handle(DeleteOpsSubscriptionCommand cmd) {
        if (!subscriptions.delete(cmd.id())) {
            throw OpsException.subscriptionNotFound(cmd.id());
        }
        return null;
    }
}
