package me.supernb.activity.app.usecase.gate.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.app.usecase.gate.GateDrawResult;

/// 金票闸机抽签命令:过闸用户触发一次(每日一次的限制在事务体内仲裁)。
///
/// @param userId 过闸用户 id;由 controller 从 @CurrentUser 解析出的 UserProfile 取值构造,不是客户端可传入的字段
public record PerformGateDrawCommand(long userId) implements Command<GateDrawResult> {
}
