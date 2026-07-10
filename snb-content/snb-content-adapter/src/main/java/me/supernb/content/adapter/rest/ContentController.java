package me.supernb.content.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import me.supernb.content.adapter.rest.request.CategoryInput;
import me.supernb.content.adapter.rest.request.UpsertArticleRequest;
import me.supernb.content.app.usecase.article.ArticleQueries;
import me.supernb.content.app.usecase.article.dto.UpsertResult;
import me.supernb.content.app.usecase.category.command.SyncCategoriesCommand;
import me.supernb.content.app.usecase.category.dto.SyncResult;
import me.supernb.content.domain.model.read.ArticleDetail;
import me.supernb.content.domain.model.read.ArticleSummary;
import me.supernb.content.domain.model.read.CategoryView;
import me.supernb.content.domain.model.read.Page;
import me.supernb.content.domain.port.repository.CategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/// 内容中心 REST：公开只读三端点 + admin 写两端点（admin 由 AdminTokenFilter 把门 + Caddy 公网 404 双保险）。
@RestController
@RequestMapping("/content/v1")
public class ContentController {

    private final ArticleQueries queries;
    private final CommandBus commandBus;

    /// 构造：读注入查询用例，写只注入 CommandBus。
    public ContentController(ArticleQueries queries, CommandBus commandBus) {
        this.queries = queries;
        this.commandBus = commandBus;
    }

    /// 可见文章分页；page 钳到 ≥1，pageSize 钳到 [1,48]（默认 12）。
    @GetMapping("/articles")
    public Page<ArticleSummary> list(@RequestParam(required = false) String category,
                                     @RequestParam(required = false) String tag,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "12") int pageSize) {
        return queries.list(category, tag, Math.max(1, page), Math.min(48, Math.max(1, pageSize)));
    }

    /// 可见文章详情；不存在/已下架 → 404（ContentException 经 trait 自动映射）。
    @GetMapping("/articles/{slug}")
    public ArticleDetail detail(@PathVariable String slug) {
        return queries.detailOrThrow(slug);
    }

    /// 全部分类（sort_order 排序，count 只计可见文章）。
    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return queries.categories();
    }

    /// admin：文章 upsert（slug 幂等；hidden 字段随身即下架/恢复）。
    @PostMapping("/admin/articles:upsert")
    public UpsertResult upsert(@RequestBody UpsertArticleRequest body) {
        return commandBus.handle(body.toCommand());
    }

    /// admin：分类整表同步（删除被引用分类会被 409 拒）。
    @PutMapping("/admin/categories")
    public SyncResult syncCategories(@RequestBody List<CategoryInput> body) {
        return commandBus.handle(new SyncCategoriesCommand(body.stream()
                .map(c -> new CategoryRepository.CategoryData(c.slug(), c.name(), c.sortOrder()))
                .toList()));
    }
}
