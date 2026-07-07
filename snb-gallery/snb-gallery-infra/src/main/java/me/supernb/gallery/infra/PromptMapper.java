package me.supernb.gallery.infra;

import me.supernb.gallery.app.GalleryDto;
import me.supernb.gallery.infra.jpa.CategoryEntity;
import me.supernb.gallery.infra.jpa.PromptEntity;

/// PromptEntity → app DTO 映射(PromptAdapter / InteractionAdapter 共用)。
final class PromptMapper {

    private PromptMapper() {
    }

    static GalleryDto.PromptSummary toSummary(PromptEntity p) {
        return new GalleryDto.PromptSummary(p.getId(), p.getTitle(), p.getImageUrl(),
                p.getImageW(), p.getImageH(), p.getAuthorName(), p.getLikeCount(), p.getFavCount());
    }

    /// 调用方须保证 category 已随查询 fetch(或为 null)。
    static GalleryDto.PromptDetail toDetail(PromptEntity p) {
        CategoryEntity c = p.getCategory();
        GalleryDto.Category category = c == null ? null
                : new GalleryDto.Category(c.getSlug(), c.getAxis(), c.getNameZh(), c.getNameEn());
        return new GalleryDto.PromptDetail(
                p.getId(), p.getSource(), p.getTitle(), p.getDescription(),
                p.getPromptText(), p.getLang(), p.getAuthorName(),
                p.getAuthorLink(), p.getSourceLink(), p.getImageUrl(),
                p.getImageW(), p.getImageH(),
                p.getLikeCount(), p.getFavCount(),
                p.getSourcePublishedAt(), p.getCreatedAt(),
                category);
    }
}
