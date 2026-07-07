package me.supernb.gallery.domain.model.read;

/// 类目树节点([CategoryTree] 三轴列表的元素),挂该类目当前已发布提示词计数。
///
/// 不带 axis 字段:节点已经按轴分装进 [CategoryTree] 的 scene/style/subject 三个列表,
/// 轴身份由所在列表决定,不需要在节点上重复携带。
///
/// @param slug   类目短标识
/// @param nameZh 中文名
/// @param nameEn 英文名
/// @param count  该类目下状态为已发布的提示词数量
public record CategoryNode(String slug, String nameZh, String nameEn, int count) {
}
