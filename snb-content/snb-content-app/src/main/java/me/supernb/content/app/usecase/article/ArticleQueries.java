package me.supernb.content.app.usecase.article;

import me.supernb.content.domain.exception.ContentException;
import me.supernb.content.domain.model.read.ArticleDetail;
import me.supernb.content.domain.model.read.ArticleSummary;
import me.supernb.content.domain.model.read.CategoryView;
import me.supernb.content.domain.model.read.Page;
import me.supernb.content.domain.port.read.ContentReadPort;
import org.springframework.stereotype.Service;

import java.util.List;

/// 内容查询服务：Controller 直接注入，读视图原样透出；404 语义收在本层。
@Service
public class ArticleQueries {

    private final ContentReadPort readPort;

    /// 构造：注入只读投影端口。
    public ArticleQueries(ContentReadPort readPort) {
        this.readPort = readPort;
    }

    /// 可见文章分页（参数钳制在 Controller 完成）。
    public Page<ArticleSummary> list(String categorySlug, String tag, int page, int pageSize) {
        return readPort.list(categorySlug, tag, page, pageSize);
    }

    /// 可见文章详情；不存在或已下架 → 404 语义异常。
    public ArticleDetail detailOrThrow(String slug) {
        return readPort.findVisibleBySlug(slug).orElseThrow(() -> ContentException.articleNotFound(slug));
    }

    /// 全部分类（含可见计数）。
    public List<CategoryView> categories() {
        return readPort.categories();
    }
}
