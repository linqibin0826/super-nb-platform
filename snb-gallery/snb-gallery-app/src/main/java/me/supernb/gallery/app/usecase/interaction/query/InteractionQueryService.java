package me.supernb.gallery.app.usecase.interaction.query;

import java.util.List;
import me.supernb.gallery.domain.model.read.MyInteractions;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.repository.InteractionRepository;
import org.springframework.stereotype.Service;

/// 互动只读查询用例,提供我的收藏分页与批量互动态回显。
@Service
public class InteractionQueryService {

    private final InteractionRepository repo;

    /// 构造:注入互动仓储端口。
    public InteractionQueryService(InteractionRepository repo) {
        this.repo = repo;
    }

    /// 「我的收藏」分页:按收藏时刻倒序,只回已发布的条目(收藏后条目被下架则从列表消失);
    /// 无收藏记录 → 空页,不是异常。
    public Page<PromptSummary> myFavorites(long userId, int page, int pageSize) {
        return repo.myFavorites(userId, page, pageSize);
    }

    /// 批量互动态回显:给定这批提示词 id,回当前用户分别赞了/藏了哪些。
    public MyInteractions myInteractions(List<Long> promptIds, long userId) {
        return repo.myInteractions(promptIds, userId);
    }
}
