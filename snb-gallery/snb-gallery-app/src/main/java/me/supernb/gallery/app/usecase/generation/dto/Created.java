package me.supernb.gallery.app.usecase.generation.dto;

import java.time.Instant;

/// 生成记录创建结果(幂等:已存在返回原 createdAt)。
public record Created(String id, Instant createdAt) {
}
