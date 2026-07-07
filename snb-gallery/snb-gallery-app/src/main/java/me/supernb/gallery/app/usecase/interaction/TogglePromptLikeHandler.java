package me.supernb.gallery.app.usecase.interaction;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptLikeCommand;
import me.supernb.gallery.app.usecase.interaction.dto.LikeResult;
import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.port.InteractionRepository;
import org.springframework.stereotype.Service;

/// 点赞开关。目标不存在/未发布 → 404;并发正确性由 InteractionRepository 实现(行锁 + 事务)保证。
@Service
public class TogglePromptLikeHandler implements CommandHandler<TogglePromptLikeCommand, LikeResult> {

    private final InteractionRepository repo;

    public TogglePromptLikeHandler(InteractionRepository repo) {
        this.repo = repo;
    }

    @Override
    public LikeResult handle(TogglePromptLikeCommand command) {
        int count = repo.toggleLike(command.promptId(), command.userId(), command.on())
                .orElseThrow(() -> GalleryException.promptNotFound(command.promptId()));
        return new LikeResult(count, command.on());
    }
}
