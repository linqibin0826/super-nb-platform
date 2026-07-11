package me.supernb.content.domain.port.read;

import me.supernb.content.domain.model.read.ArticleDetail;
import me.supernb.content.domain.model.read.ArticleSummary;
import me.supernb.content.domain.model.read.CategoryView;
import me.supernb.content.domain.model.read.Page;

import java.util.List;
import java.util.Optional;

/// content 只读查询端口（公开接口的数据面，hidden 一律不可见）。
public interface ContentReadPort {

    /// 可见文章分页：published_at 倒序；category/tag 留空则不参与对应过滤维度。
    Page<ArticleSummary> list(String categorySlug, String tag, int page, int pageSize);

    /// 可见文章详情；不存在或 hidden → empty（对外统一 404，不泄露下架状态）。
    Optional<ArticleDetail> findVisibleBySlug(String slug);

    /// 全部分类（sort_order 排序），count 只计可见文章。
    List<CategoryView> categories();
}
