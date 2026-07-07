package me.supernb.gallery.domain.model.read;

/// 类目(内嵌进 [PromptDetail]),不带发布计数——计数只在类目树的 [CategoryNode] 里出现。
///
/// @param slug   类目短标识(全局唯一)
/// @param axis   类目轴:`scene` | `style` | `subject`
/// @param nameZh 中文名
/// @param nameEn 英文名
public record Category(String slug, String axis, String nameZh, String nameEn) {
}
