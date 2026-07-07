package me.supernb.gallery.domain.model.read;

import java.util.List;

/// 统一分页信封,对外 JSON `{items,total,page,pages}`。与 Spring Data 的同名分页类型无关——
/// infra 层常见两者一起出现,需要靠 import 消歧义(FQN 是本仓唯一允许的例外场景)。
///
/// @param items 本页数据
/// @param total 总条数
/// @param page  当前页码(1 起)
/// @param pages 总页数(由 `of()` 按 total/pageSize 换算得出;pageSize 本身不落在这个信封里)
public record Page<T>(List<T> items, long total, int page, int pages) {
    /// 按总条数与每页大小算出总页数并组装分页信封;pageSize ≤ 0 时总页数记 0(不做除零)。
    public static <T> Page<T> of(List<T> items, long total, int page, int pageSize) {
        int pages = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new Page<>(items, total, page, pages);
    }
}
