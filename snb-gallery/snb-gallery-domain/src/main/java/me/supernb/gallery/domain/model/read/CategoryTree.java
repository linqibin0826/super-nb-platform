package me.supernb.gallery.domain.model.read;

import java.util.List;

/// 三轴类目树。
public record CategoryTree(List<CategoryNode> scene, List<CategoryNode> style, List<CategoryNode> subject) {
}
