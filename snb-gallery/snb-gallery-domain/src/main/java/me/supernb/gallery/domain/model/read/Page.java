package me.supernb.gallery.domain.model.read;

import java.util.List;

/// 统一分页信封,对外 JSON {items,total,page,pages}。
public record Page<T>(List<T> items, long total, int page, int pages) {
    public static <T> Page<T> of(List<T> items, long total, int page, int pageSize) {
        int pages = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new Page<>(items, total, page, pages);
    }
}
