package me.supernb.ops.app.usecase.subscription.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;

/// 改订阅命令(全量覆盖)。
public record UpdateOpsSubscriptionCommand(long id, SubscriptionData data) implements Command<Void> {
}
