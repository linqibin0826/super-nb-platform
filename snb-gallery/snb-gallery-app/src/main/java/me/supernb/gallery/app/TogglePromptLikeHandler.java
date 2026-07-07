package me.supernb.gallery.app;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.gallery.domain.GalleryException;
import org.springframework.stereotype.Service;

/// 点赞开关。目标不存在/未发布 → 404;并发正确性由 InteractionRepository 实现(行锁 + 事务)保证。
@Service
public class TogglePromptLikeHandler implements CommandHandler<TogglePromptLikeCommand, GalleryDto.LikeResult> {

    private final InteractionRepository repo;

    public TogglePromptLikeHandler(InteractionRepository repo) {
        this.repo = repo;
    }

    @Override
    public GalleryDto.LikeResult handle(TogglePromptLikeCommand command) {
        int count = repo.toggleLike(command.promptId(), command.userId(), command.on())
                .orElseThrow(() -> GalleryException.promptNotFound(command.promptId()));
        return new GalleryDto.LikeResult(count, command.on());
    }
}
