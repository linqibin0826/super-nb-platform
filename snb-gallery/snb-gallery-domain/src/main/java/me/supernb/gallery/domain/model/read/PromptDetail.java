package me.supernb.gallery.domain.model.read;

import java.time.Instant;

/// 提示词详情:[PromptSummary] 的全字段版本,含正文与类目。
///
/// @param id                提示词 id(雪花的字符串形式)
/// @param source            数据来源:`youmind` | `ff` | `ym` | `own`
/// @param title             标题
/// @param description       描述
/// @param promptText        提示词正文([PromptSummary] 不带这个字段)
/// @param lang              语种
/// @param authorName        作者名
/// @param authorLink        作者主页链接
/// @param sourceLink        来源页链接
/// @param imageUrl          预览图 CDN 完整 URL
/// @param imageW            预览图宽(px),可空
/// @param imageH            预览图高(px),可空
/// @param likeCount         点赞数(反规范化计数)
/// @param favCount          收藏数(反规范化计数)
/// @param sourcePublishedAt 来源侧原始发布时间,可空(部分条目缺该信息)
/// @param createdAt         本平台收录(落库)时刻——与 sourcePublishedAt 是两条不同的时间轴
/// @param category          所属类目,可空
public record PromptDetail(
        String id, String source, String title, String description, String promptText,
        String lang, String authorName, String authorLink, String sourceLink,
        String imageUrl, Integer imageW, Integer imageH,
        int likeCount, int favCount, Instant sourcePublishedAt, Instant createdAt,
        Category category) {
}
