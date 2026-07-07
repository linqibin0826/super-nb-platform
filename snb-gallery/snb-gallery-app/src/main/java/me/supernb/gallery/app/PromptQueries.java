package me.supernb.gallery.app;

import me.supernb.gallery.domain.GalleryException;
import me.supernb.gallery.domain.SortMode;
import org.springframework.stereotype.Service;

/// 提示词只读查询用例。
@Service
public class PromptQueries {

    private final PromptRepository repo;

    public PromptQueries(PromptRepository repo) {
        this.repo = repo;
    }

    public GalleryDto.Page<GalleryDto.PromptSummary> list(
            String categorySlug, String q, String sort, int page, int pageSize) {
        return repo.list(categorySlug, q, SortMode.from(sort), page, pageSize);
    }

    public GalleryDto.PromptDetail detail(long id) {
        return repo.detail(id).orElseThrow(() -> GalleryException.promptNotFound(id));
    }

    public GalleryDto.CategoryTree categories() {
        return repo.categories();
    }
}
