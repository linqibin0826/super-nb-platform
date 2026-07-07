package me.supernb.gallery.app.usecase.interaction.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.gallery.app.usecase.interaction.dto.LikeResult;

/// 点赞开关命令:on=true 赞、false 取消。toggle 幂等(重复操作不重复计数)。
public record TogglePromptLikeCommand(long promptId, long userId, boolean on)
        implements Command<LikeResult> {
}
