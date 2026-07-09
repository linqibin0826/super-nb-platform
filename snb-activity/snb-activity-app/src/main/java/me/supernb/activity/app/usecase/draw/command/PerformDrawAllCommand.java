package me.supernb.activity.app.usecase.draw.command;

import dev.linqibin.commons.cqrs.Command;
import java.util.List;
import me.supernb.activity.domain.model.DrawResult;

/// 批量抽奖命令:当前登录用户在进行中活动里一次抽 min(剩余, 10)。
///
/// 返回类型复用 domain 既有 DrawResult 的列表,不新立结果类型;经 CommandBus 派发给
/// 同子域包下的 PerformDrawAllHandler。
///
/// @param userId 发起抽奖的用户 id;由 controller 从 @CurrentUser 取值构造,非客户端可传字段
public record PerformDrawAllCommand(long userId) implements Command<List<DrawResult>> {
}
