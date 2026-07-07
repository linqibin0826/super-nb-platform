package me.supernb.gallery.domain.model.read;

import java.util.List;

/// 三轴类目树:提示词类目按 scene(场景)/style(风格)/subject(主体)三个轴分组返回。
///
/// @param scene   场景轴类目列表
/// @param style   风格轴类目列表
/// @param subject 主体轴类目列表
public record CategoryTree(List<CategoryNode> scene, List<CategoryNode> style, List<CategoryNode> subject) {
}
