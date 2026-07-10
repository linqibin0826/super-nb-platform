package me.supernb.content.domain.port.repository;

import java.time.Instant;
import java.util.List;

/// 文章聚合的写端口：以 slug 为幂等键 upsert（发布管线改稿重发=同 slug 再来一次）。
public interface ArticleRepository {

    /// slug 已存在则全量覆盖其余字段（id 不变），否则新建（应用层雪花取号）。
    UpsertOutcome upsert(ArticleData data);

    /// 发布管线送来的文章全量字段；type 只有 article/ebook 两值（管线按目录判定）。
    record ArticleData(String slug, String type, String title, String summary, String coverUrl,
                       String categorySlug, List<String> tags, String bodyHtml, String ebookPath,
                       String sourceName, String sourceUrl, Instant publishedAt, boolean hidden) {}

    /// upsert 结果：落库 id + 本次是否新建。
    record UpsertOutcome(long id, boolean created) {}
}
