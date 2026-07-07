package me.supernb.gallery.infra.adapter.read;

import me.supernb.gallery.domain.model.read.Category;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.infra.adapter.persistence.entity.CategoryEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;

/// PromptEntity → app DTO 映射(PromptReadAdapter / InteractionRepositoryAdapter 共用)。
public final class PromptMapper {

    /// 工具类不实例化。
    private PromptMapper() {
    }

    /// 实体 → 列表瘦身条目。
    public static PromptSummary toSummary(PromptEntity p) {
        return new PromptSummary(String.valueOf(p.getId()), p.getTitle(), p.getImageUrl(),
                p.getImageW(), p.getImageH(), p.getAuthorName(), p.getLikeCount(), p.getFavCount());
    }

    /// 调用方须保证 category 已随查询 fetch(或为 null)。
    public static PromptDetail toDetail(PromptEntity p) {
        CategoryEntity c = p.getCategory();
        Category category = c == null ? null
                : new Category(c.getSlug(), c.getAxis(), c.getNameZh(), c.getNameEn());
        return new PromptDetail(
                String.valueOf(p.getId()), p.getSource(), p.getTitle(), p.getDescription(),
                p.getPromptText(), p.getLang(), p.getAuthorName(),
                p.getAuthorLink(), p.getSourceLink(), p.getImageUrl(),
                p.getImageW(), p.getImageH(),
                p.getLikeCount(), p.getFavCount(),
                p.getSourcePublishedAt(), p.getCreatedAt(),
                category);
    }
}
