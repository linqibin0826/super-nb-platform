package me.supernb.gallery.app.usecase.prompt.query;

import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.model.enums.SortMode;
import me.supernb.gallery.domain.model.read.CategoryTree;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.read.PromptReadPort;
import org.springframework.stereotype.Service;

/// 提示词只读查询用例。
@Service
public class PromptQueryService {

    private final PromptReadPort repo;

    /// 构造:注入提示词读端口。
    public PromptQueryService(PromptReadPort repo) {
        this.repo = repo;
    }

    public Page<PromptSummary> list(
            String categorySlug, String q, String sort, int page, int pageSize) {
        return repo.list(categorySlug, q, SortMode.from(sort), page, pageSize);
    }

    /// 单条详情,不存在/未发布 → 404。
    public PromptDetail detail(long id) {
        return repo.detail(id).orElseThrow(() -> GalleryException.promptNotFound(id));
    }

    /// 三轴类目树(带已发布计数)。
    public CategoryTree categories() {
        return repo.categories();
    }
}
