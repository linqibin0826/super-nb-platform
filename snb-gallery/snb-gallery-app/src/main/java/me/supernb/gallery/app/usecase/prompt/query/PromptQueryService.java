package me.supernb.gallery.app.usecase.prompt.query;

import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.model.enums.SortMode;
import me.supernb.gallery.domain.model.read.CategoryTree;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.read.PromptReadPort;
import org.springframework.stereotype.Service;

/// 提示词只读查询用例:列表分页、单条详情、三轴类目树。
@Service
public class PromptQueryService {

    private final PromptReadPort repo;

    /// 构造:注入提示词读端口。
    public PromptQueryService(PromptReadPort repo) {
        this.repo = repo;
    }

    /// 已发布提示词分页:categorySlug 过滤类目、q 按标题/描述做 ILIKE 搜索,
    /// sort 未知值回退 featured(收录序);无匹配 → 空页,不是异常。
    public Page<PromptSummary> list(
            String categorySlug, String q, String sort, int page, int pageSize) {
        return repo.list(categorySlug, q, SortMode.from(sort), page, pageSize);
    }

    /// 提示词详情,不存在或未发布 → 404。
    public PromptDetail detail(long id) {
        return repo.detail(id).orElseThrow(() -> GalleryException.promptNotFound(id));
    }

    /// 三轴类目树,每个类目节点带已发布条目计数。
    public CategoryTree categories() {
        return repo.categories();
    }
}
