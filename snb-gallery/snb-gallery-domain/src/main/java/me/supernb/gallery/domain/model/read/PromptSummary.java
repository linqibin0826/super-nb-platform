package me.supernb.gallery.domain.model.read;

/// 列表瘦身条目(不含 prompt 全文)。
public record PromptSummary(
        long id, String title, String imageUrl, Integer imageW, Integer imageH,
        String authorName, int likeCount, int favCount) {
}
