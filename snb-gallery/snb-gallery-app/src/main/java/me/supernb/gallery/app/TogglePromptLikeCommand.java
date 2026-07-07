package me.supernb.gallery.app;

import dev.linqibin.commons.cqrs.Command;

/// 点赞开关命令:on=true 赞、false 取消。toggle 幂等(重复操作不重复计数)。
public record TogglePromptLikeCommand(long promptId, long userId, boolean on)
        implements Command<GalleryDto.LikeResult> {
}
