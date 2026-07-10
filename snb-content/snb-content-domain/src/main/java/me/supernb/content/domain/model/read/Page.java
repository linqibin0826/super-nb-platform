package me.supernb.content.domain.model.read;

import java.util.List;

/// 分页信封（照库内 gallery 惯例；content 自持一份——跨上下文禁依赖，ArchUnit 门禁）。
public record Page<T>(List<T> items, long total, int page, int pages) {

    /// 按 pageSize 折算总页数（至少 1 页，空结果也返回 page=1/pages=1 的空信封）。
    public static <T> Page<T> of(List<T> items, long total, int page, int pageSize) {
        return new Page<>(items, total, page, (int) Math.max(1, (total + pageSize - 1) / pageSize));
    }
}
