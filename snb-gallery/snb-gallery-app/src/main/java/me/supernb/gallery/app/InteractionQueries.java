package me.supernb.gallery.app;

import java.util.List;
import org.springframework.stereotype.Service;

/// 互动只读查询用例:我的收藏分页、批量互动态回显。
@Service
public class InteractionQueries {

    private final InteractionRepository repo;

    public InteractionQueries(InteractionRepository repo) {
        this.repo = repo;
    }

    public GalleryDto.Page<GalleryDto.PromptSummary> myFavorites(long userId, int page, int pageSize) {
        return repo.myFavorites(userId, page, pageSize);
    }

    public GalleryDto.MyInteractions myInteractions(List<Long> promptIds, long userId) {
        return repo.myInteractions(promptIds, userId);
    }
}
