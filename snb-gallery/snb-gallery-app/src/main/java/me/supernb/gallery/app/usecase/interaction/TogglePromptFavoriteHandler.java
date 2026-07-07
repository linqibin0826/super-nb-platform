package me.supernb.gallery.app.usecase.interaction;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptFavoriteCommand;
import me.supernb.gallery.app.usecase.interaction.dto.FavResult;
import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.port.repository.InteractionRepository;
import org.springframework.stereotype.Service;

/// 收藏开关。目标不存在或未发布 → 404;toggle 的并发正确性与幂等由 InteractionRepository
/// 实现保证(成员表唯一约束撞车 → 整事务回滚 → 事务外回读最新计数),本类只负责编排。
@Service
public class TogglePromptFavoriteHandler
        implements CommandHandler<TogglePromptFavoriteCommand, FavResult> {

    private final InteractionRepository repo;

    /// 构造:注入互动仓储端口。
    public TogglePromptFavoriteHandler(InteractionRepository repo) {
        this.repo = repo;
    }

    /// 收藏开关,目标不存在或未发布 → 404。
    @Override
    public FavResult handle(TogglePromptFavoriteCommand command) {
        int count = repo.toggleFavorite(command.promptId(), command.userId(), command.on())
                .orElseThrow(() -> GalleryException.promptNotFound(command.promptId()));
        return new FavResult(count, command.on());
    }
}
