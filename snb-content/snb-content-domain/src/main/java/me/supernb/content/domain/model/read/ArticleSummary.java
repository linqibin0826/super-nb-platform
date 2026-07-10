package me.supernb.content.domain.model.read;

import java.time.Instant;
import java.util.List;

/// 列表卡片视图（不含正文）。coverUrl 可空=纯文字卡；sourceName 可空=非搬运内容。
public record ArticleSummary(String id, String slug, String type, String title, String summary,
                             String coverUrl, String categorySlug, String categoryName,
                             List<String> tags, String sourceName, Instant publishedAt) {
}
