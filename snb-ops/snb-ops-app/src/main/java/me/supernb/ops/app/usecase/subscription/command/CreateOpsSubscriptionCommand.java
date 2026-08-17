package me.supernb.ops.app.usecase.subscription.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;

/// 建订阅命令(订阅无密文字段,直接复用端口 record);返回新订阅 id 字符串。
public record CreateOpsSubscriptionCommand(SubscriptionData data) implements Command<String> {
}
