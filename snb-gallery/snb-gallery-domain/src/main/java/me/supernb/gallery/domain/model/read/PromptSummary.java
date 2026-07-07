package me.supernb.gallery.domain.model.read;

/// 提示词列表条目:[PromptDetail] 的瘦身版,不含正文/描述/类目等详情页才需要的字段。
///
/// @param id         提示词 id(雪花的字符串形式)
/// @param title      标题
/// @param imageUrl   预览图 CDN 完整 URL
/// @param imageW     预览图宽(px),可空
/// @param imageH     预览图高(px),可空
/// @param authorName 作者名
/// @param likeCount  点赞数(反规范化计数)
/// @param favCount   收藏数(反规范化计数)
public record PromptSummary(
        String id, String title, String imageUrl, Integer imageW, Integer imageH,
        String authorName, int likeCount, int favCount) {
}
