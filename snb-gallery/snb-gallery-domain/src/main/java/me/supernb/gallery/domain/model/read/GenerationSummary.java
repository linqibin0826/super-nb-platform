package me.supernb.gallery.domain.model.read;

import java.time.Instant;

/// 生成历史列表条目(thumbUrl 已现签)。
public record GenerationSummary(
        String id, Instant createdAt, String prompt, String size, int n, String quality,
        String status, Double cost, int elapsedMs, String error, String thumbUrl) {
}
