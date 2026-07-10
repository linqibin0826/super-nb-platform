package me.supernb.content.app.usecase.article.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.content.app.usecase.article.dto.UpsertResult;

import java.time.Instant;
import java.util.List;

/// 发布管线的文章 upsert 命令（slug 幂等键；tags 可为 null，处理器归一化为空表）。
public record UpsertArticleCommand(String slug, String type, String title, String summary, String coverUrl,
                                   String categorySlug, List<String> tags, String bodyHtml, String ebookPath,
                                   String sourceName, String sourceUrl, Instant publishedAt, boolean hidden)
        implements Command<UpsertResult> {
}
