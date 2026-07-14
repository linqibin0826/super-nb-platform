package me.supernb.activity.app.usecase.checkin.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.domain.model.checkin.CheckInResult;

/// 签到命令:userId 来自登录态,由 controller 从 @CurrentUser 取值构造,非客户端可传字段。
public record CheckInCommand(long userId) implements Command<CheckInResult> {
}
