package me.supernb.activity.app.usecase.thursday.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.app.usecase.thursday.ThursdayGuessView;

/// 猜桶竞猜提交命令。
///
/// @param userId 提交人;由 controller 从 @CurrentUser 取,不是客户端可传入的字段
/// @param guess  猜的份数;取值合法性在 handler 里按桶上限校验,不信客户端
public record SubmitThursdayGuessCommand(long userId, int guess) implements Command<ThursdayGuessView> {
}
