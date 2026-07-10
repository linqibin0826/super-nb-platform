package me.supernb.content.adapter.rest.request;

import me.supernb.content.app.usecase.article.command.UpsertArticleCommand;

import java.time.Instant;
import java.util.List;

/// 发布管线的文章 upsert 请求体（字段与命令一一对应；tags 可缺省，处理器归一化）。
public record UpsertArticleRequest(String slug, String type, String title, String summary, String coverUrl,
                                   String categorySlug, List<String> tags, String bodyHtml, String ebookPath,
                                   String sourceName, String sourceUrl, Instant publishedAt, boolean hidden) {

    /// 转 app 层命令。
    public UpsertArticleCommand toCommand() {
        return new UpsertArticleCommand(slug, type, title, summary, coverUrl, categorySlug, tags,
                bodyHtml, ebookPath, sourceName, sourceUrl, publishedAt, hidden);
    }
}
