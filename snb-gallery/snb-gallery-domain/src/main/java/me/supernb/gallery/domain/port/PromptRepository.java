package me.supernb.gallery.domain.port;

import java.util.Optional;
import me.supernb.gallery.domain.model.enums.SortMode;
import me.supernb.gallery.domain.model.read.CategoryTree;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;

/// 提示词只读仓储端口(gallery 库)。
public interface PromptRepository {

    /// 已发布提示词分页(类目 slug 过滤 + q 标题/描述 ILIKE 搜索 + 排序)。
    Page<PromptSummary> list(
            String categorySlug, String q, SortMode sort, int page, int pageSize);

    /// 单条详情(全字段);不存在或未发布 → empty。
    Optional<PromptDetail> detail(long id);

    /// 三轴类目树(含各类目已发布计数)。
    CategoryTree categories();
}
