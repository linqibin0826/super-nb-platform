package me.supernb.gallery.domain.model.read;

import java.time.Instant;

/// 生成历史列表条目:[GenerationDetail] 的瘦身版,不含分组/key/输出参考图明细,只留列表要渲染的字段。
///
/// @param id        生成记录 id(雪花的字符串形式)
/// @param createdAt 落库(创建)时刻
/// @param prompt    生成用提示词
/// @param size      尺寸档,如 `1024x1024`
/// @param n         出图张数
/// @param quality   画质档
/// @param status    任务终态:`done` | `error`
/// @param cost      本次消耗额度(USD 名义计价),可空
/// @param elapsedMs 生成耗时毫秒
/// @param error     失败原因;成功时为 null
/// @param thumbUrl  256px 缩略图 presigned URL,可空——生成失败且没有任何输出图时无图可现签
public record GenerationSummary(
        String id, Instant createdAt, String prompt, String size, int n, String quality,
        String status, Double cost, int elapsedMs, String error, String thumbUrl) {
}
