package me.supernb.gallery.domain.port.read;

import java.util.Optional;
import me.supernb.gallery.domain.model.enums.SortMode;
import me.supernb.gallery.domain.model.read.CategoryTree;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;

/// 提示词读投影端口:gallery 库已发布提示词的列表/详情/类目树三类只读查询。
public interface PromptReadPort {

    /// 已发布提示词分页:类目 slug 精确过滤 + 关键字对标题/描述做 ILIKE 模糊匹配 + 排序,
    /// categorySlug/q 均可为空(不过滤);无匹配 → 空列表,不是异常。
    Page<PromptSummary> list(
            String categorySlug, String q, SortMode sort, int page, int pageSize);

    /// 单条详情(全字段);不存在或未发布 → empty(由用例转 404)。
    Optional<PromptDetail> detail(long id);

    /// 三轴类目树,含各类目当前已发布提示词计数。
    CategoryTree categories();
}
