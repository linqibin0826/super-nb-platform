package me.supernb.gallery.infra.adapter.read;

import me.supernb.gallery.domain.model.read.Category;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.infra.adapter.persistence.entity.CategoryEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;

/// PromptEntity → domain 读视图的手写映射,[PromptReadAdapter] 与 `InteractionRepositoryAdapter` 共用。
public final class PromptMapper {

    /// 工具类,禁止实例化。
    private PromptMapper() {
    }

    /// 实体 → 列表用的瘦身条目。
    public static PromptSummary toSummary(PromptEntity p) {
        return new PromptSummary(String.valueOf(p.getId()), p.getTitle(), p.getImageUrl(),
                p.getImageW(), p.getImageH(), p.getAuthorName(), p.getLikeCount(), p.getFavCount());
    }

    /// 实体 → 单条详情;category 是懒加载关联,调用方须保证查询已 join fetch(或该条目本就未挂类目、为 null),
    /// 否则越界访问会触发懒加载异常。
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
