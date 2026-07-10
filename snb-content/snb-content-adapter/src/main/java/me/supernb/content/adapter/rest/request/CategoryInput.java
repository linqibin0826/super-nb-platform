package me.supernb.content.adapter.rest.request;

/// 分类整表同步的一项（categories.yml 的一条）。
public record CategoryInput(String slug, String name, int sortOrder) {
}
