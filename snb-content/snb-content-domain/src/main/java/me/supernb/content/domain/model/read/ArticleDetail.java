package me.supernb.content.domain.model.read;

import java.time.Instant;
import java.util.List;

/// 详情视图：article 型带 bodyHtml（管线预渲染），ebook 型带 ebookPath（books/<slug>.html）。
public record ArticleDetail(String id, String slug, String type, String title, String summary,
                            String coverUrl, String categorySlug, String categoryName,
                            List<String> tags, String bodyHtml, String ebookPath,
                            String sourceName, String sourceUrl, Instant publishedAt) {
}
