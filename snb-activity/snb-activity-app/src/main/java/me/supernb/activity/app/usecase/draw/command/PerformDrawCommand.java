package me.supernb.activity.app.usecase.draw.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.domain.model.DrawResult;

/// 抽奖命令:当前登录用户在进行中活动里抽一次。
///
/// 返回类型复用 domain 既有的 DrawResult,不新立命令专属结果类型;
/// 经 CommandBus 派发给同子域包下的 PerformDrawHandler。
///
/// @param userId 发起抽奖的用户 id;由 controller 从 @CurrentUser 解析出的 UserProfile 取值构造,不是客户端可传入的字段
public record PerformDrawCommand(long userId) implements Command<DrawResult> {
}
