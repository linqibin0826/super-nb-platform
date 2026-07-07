package me.supernb.gallery.app;

import java.util.Optional;
import me.supernb.gallery.domain.SortMode;

/// 提示词只读仓储端口(gallery 库)。
public interface PromptRepository {

    /// 已发布提示词分页(类目 slug 过滤 + q 标题/描述 ILIKE 搜索 + 排序)。
    GalleryDto.Page<GalleryDto.PromptSummary> list(
            String categorySlug, String q, SortMode sort, int page, int pageSize);

    /// 单条详情(全字段);不存在或未发布 → empty。
    Optional<GalleryDto.PromptDetail> detail(long id);

    /// 三轴类目树(含各类目已发布计数)。
    GalleryDto.CategoryTree categories();
}
