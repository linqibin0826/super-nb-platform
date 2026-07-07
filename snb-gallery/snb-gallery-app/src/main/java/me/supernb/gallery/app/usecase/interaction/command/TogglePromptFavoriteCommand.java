package me.supernb.gallery.app.usecase.interaction.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.gallery.app.usecase.interaction.dto.FavResult;

/// 收藏开关命令。
///
/// toggle 幂等:同一 (promptId, userId) 重复提交相同 on 值不会重复计数,
/// 幂等性由 InteractionRepository 实现保证,命令本身只是数据载体。
///
/// @param promptId 目标提示词 id
/// @param userId   操作用户 id
/// @param on       true=收藏、false=取消收藏
public record TogglePromptFavoriteCommand(long promptId, long userId, boolean on)
        implements Command<FavResult> {
}
