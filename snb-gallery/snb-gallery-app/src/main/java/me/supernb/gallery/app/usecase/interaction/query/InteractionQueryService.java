package me.supernb.gallery.app.usecase.interaction.query;

import java.util.List;
import me.supernb.gallery.domain.model.read.MyInteractions;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.repository.InteractionRepository;
import org.springframework.stereotype.Service;

/// 互动只读查询用例:我的收藏分页、批量互动态回显。
@Service
public class InteractionQueryService {

    private final InteractionRepository repo;

    /// 构造:注入互动仓储端口。
    public InteractionQueryService(InteractionRepository repo) {
        this.repo = repo;
    }

    public Page<PromptSummary> myFavorites(long userId, int page, int pageSize) {
        return repo.myFavorites(userId, page, pageSize);
    }

    /// 批量互动态回显(这批 id 里我赞/藏了哪些)。
    public MyInteractions myInteractions(List<Long> promptIds, long userId) {
        return repo.myInteractions(promptIds, userId);
    }
}
