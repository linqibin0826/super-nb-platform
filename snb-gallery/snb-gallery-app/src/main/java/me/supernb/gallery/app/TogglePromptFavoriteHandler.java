package me.supernb.gallery.app;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.gallery.domain.GalleryException;
import org.springframework.stereotype.Service;

/// 收藏开关。目标不存在/未发布 → 404;并发正确性由 InteractionRepository 实现(行锁 + 事务)保证。
@Service
public class TogglePromptFavoriteHandler
        implements CommandHandler<TogglePromptFavoriteCommand, GalleryDto.FavResult> {

    private final InteractionRepository repo;

    public TogglePromptFavoriteHandler(InteractionRepository repo) {
        this.repo = repo;
    }

    @Override
    public GalleryDto.FavResult handle(TogglePromptFavoriteCommand command) {
        int count = repo.toggleFavorite(command.promptId(), command.userId(), command.on())
                .orElseThrow(() -> GalleryException.promptNotFound(command.promptId()));
        return new GalleryDto.FavResult(count, command.on());
    }
}
