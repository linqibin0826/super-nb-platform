package me.supernb.content.domain.model.read;

/// 分类视图：count 只计可见（非 hidden）文章，供列表页 tab 使用。
public record CategoryView(String slug, String name, int sortOrder, long count) {
}
