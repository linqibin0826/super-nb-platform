package me.supernb.gallery.app.usecase.prompt.query;

import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.model.enums.SortMode;
import me.supernb.gallery.domain.model.read.CategoryTree;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.PromptRepository;
import org.springframework.stereotype.Service;

/// 提示词只读查询用例。
@Service
public class PromptQueries {

    private final PromptRepository repo;

    public PromptQueries(PromptRepository repo) {
        this.repo = repo;
    }

    public Page<PromptSummary> list(
            String categorySlug, String q, String sort, int page, int pageSize) {
        return repo.list(categorySlug, q, SortMode.from(sort), page, pageSize);
    }

    public PromptDetail detail(long id) {
        return repo.detail(id).orElseThrow(() -> GalleryException.promptNotFound(id));
    }

    public CategoryTree categories() {
        return repo.categories();
    }
}
