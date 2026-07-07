package me.supernb.gallery.domain.model.read;

/// 类目树节点(带该类目已发布计数)。
public record CategoryNode(String slug, String nameZh, String nameEn, int count) {
}
