package me.supernb.gallery.app.usecase.generation.dto;

import java.time.Instant;

/// 创建生成记录的写结果。每次创建都会取一个新雪花 id、落一条新记录(没有创建级幂等,
/// 见 `CreateGenerationCommand`);id 已字符串化(雪花超 JS 安全整数,验收意见⑦)。
///
/// @param id        生成记录 id(字符串化的雪花 id)
/// @param createdAt 记录落库时刻
public record Created(String id, Instant createdAt) {
}
