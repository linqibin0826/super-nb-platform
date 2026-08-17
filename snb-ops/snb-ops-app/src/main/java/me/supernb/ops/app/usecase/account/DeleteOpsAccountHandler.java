package me.supernb.ops.app.usecase.account;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.ops.app.usecase.account.command.DeleteOpsAccountCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import org.springframework.stereotype.Service;

/// 删账号用例:名下有订阅一律拒(先删订阅再删账号,防误删带走生命周期记录)。
@Service
public class DeleteOpsAccountHandler implements CommandHandler<DeleteOpsAccountCommand, Void> {

    private final OpsAccountRepository accounts;
    private final OpsSubscriptionRepository subscriptions;

    /// 构造:注入两仓储端口。
    public DeleteOpsAccountHandler(OpsAccountRepository accounts, OpsSubscriptionRepository subscriptions) {
        this.accounts = accounts;
        this.subscriptions = subscriptions;
    }

    @Override
    public Void handle(DeleteOpsAccountCommand cmd) {
        if (subscriptions.countByAccount(cmd.id()) > 0) {
            throw OpsException.accountHasSubscriptions(cmd.id());
        }
        if (!accounts.delete(cmd.id())) {
            throw OpsException.accountNotFound(cmd.id());
        }
        return null;
    }
}
