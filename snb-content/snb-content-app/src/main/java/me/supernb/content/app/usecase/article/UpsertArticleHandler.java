package me.supernb.content.app.usecase.article;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.content.app.usecase.article.command.UpsertArticleCommand;
import me.supernb.content.app.usecase.article.dto.UpsertResult;
import me.supernb.content.domain.exception.ContentException;
import me.supernb.content.domain.port.repository.ArticleRepository;
import me.supernb.content.domain.port.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/// 文章 upsert 用例：分类存在性守卫 → tags 归一化 → 委托仓储 → id String 化返回。
///
/// slug/标题等格式合法性由发布管线前置校验，本层只守卫会破坏引用完整性的分类维度。
@Service
public class UpsertArticleHandler implements CommandHandler<UpsertArticleCommand, UpsertResult> {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;

    /// 构造：注入文章/分类写端口。
    public UpsertArticleHandler(ArticleRepository articleRepository, CategoryRepository categoryRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
    }

    /// 处理 upsert 命令；分类不存在抛 404 语义异常（管线侧应先 sync categories.yml）。
    @Override
    public UpsertResult handle(UpsertArticleCommand cmd) {
        if (!categoryRepository.exists(cmd.categorySlug())) {
            throw ContentException.categoryNotFound(cmd.categorySlug());
        }
        List<String> tags = cmd.tags() == null ? List.of() : List.copyOf(cmd.tags());
        ArticleRepository.UpsertOutcome outcome = articleRepository.upsert(new ArticleRepository.ArticleData(
                cmd.slug(), cmd.type(), cmd.title(), cmd.summary(), cmd.coverUrl(),
                cmd.categorySlug(), tags, cmd.bodyHtml(), cmd.ebookPath(),
                cmd.sourceName(), cmd.sourceUrl(), cmd.publishedAt(), cmd.hidden()));
        return new UpsertResult(String.valueOf(outcome.id()), outcome.created());
    }
}
