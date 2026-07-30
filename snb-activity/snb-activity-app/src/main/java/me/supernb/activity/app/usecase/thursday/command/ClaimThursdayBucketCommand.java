package me.supernb.activity.app.usecase.thursday.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.app.usecase.thursday.ThursdayBucketView;

/// 疯四桶领取命令。
///
/// @param userId 领取用户 id;由 controller 从 @CurrentUser 解析出的 UserProfile 取值构造,
///               不是客户端可传入的字段——否则任何人都能替别人领(或替自己冒领别人的桶序)
public record ClaimThursdayBucketCommand(long userId) implements Command<ThursdayBucketView> {
}
