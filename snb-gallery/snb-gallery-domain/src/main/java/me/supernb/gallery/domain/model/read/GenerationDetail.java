package me.supernb.gallery.domain.model.read;

import java.time.Instant;
import java.util.List;

/// 生成历史详情(全字段 + 输出图 + 参考图)。
public record GenerationDetail(
        String id, Instant createdAt, String prompt, String size, int n, String quality,
        String status, Double cost, int elapsedMs, String groupName, Long keyId, String error,
        List<Image> outputImages, List<Image> refImages) {
}
