package me.supernb.gallery.domain.model.read;

import java.time.Instant;

/// 单条详情(全字段 + 类目)。
public record PromptDetail(
        String id, String source, String title, String description, String promptText,
        String lang, String authorName, String authorLink, String sourceLink,
        String imageUrl, Integer imageW, Integer imageH,
        int likeCount, int favCount, Instant sourcePublishedAt, Instant createdAt,
        Category category) {
}
