package me.supernb.gallery.app;

import java.util.List;
import me.supernb.gallery.domain.GalleryException;
import org.springframework.stereotype.Service;

/// 点赞/收藏用例。toggle 幂等,目标不存在/未发布 → 404。
@Service
public class Interactions {

    private final InteractionRepository repo;

    public Interactions(InteractionRepository repo) {
        this.repo = repo;
    }

    public record LikeResult(int likeCount, boolean liked) {
    }

    public record FavResult(int favCount, boolean favorited) {
    }

    public LikeResult like(long promptId, long userId, boolean on) {
        int count = repo.toggleLike(promptId, userId, on)
                .orElseThrow(() -> GalleryException.promptNotFound(promptId));
        return new LikeResult(count, on);
    }

    public FavResult favorite(long promptId, long userId, boolean on) {
        int count = repo.toggleFavorite(promptId, userId, on)
                .orElseThrow(() -> GalleryException.promptNotFound(promptId));
        return new FavResult(count, on);
    }

    public GalleryDto.Page<GalleryDto.PromptSummary> myFavorites(long userId, int page, int pageSize) {
        return repo.myFavorites(userId, page, pageSize);
    }

    public GalleryDto.MyInteractions myInteractions(List<Long> promptIds, long userId) {
        return repo.myInteractions(promptIds, userId);
    }
}
