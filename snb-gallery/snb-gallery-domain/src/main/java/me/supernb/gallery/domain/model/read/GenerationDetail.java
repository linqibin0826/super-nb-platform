package me.supernb.gallery.domain.model.read;

import java.time.Instant;
import java.util.List;

/// 生成记录详情:聚合根全字段 + 输出图 + 参考图(图片 url 均已现签 presigned,可直接前端展示)。
///
/// @param id           生成记录 id(雪花的字符串形式,对外 id 契约)
/// @param createdAt    落库(创建)时刻
/// @param prompt       生成用提示词
/// @param size         尺寸档,如 `1024x1024`
/// @param n            出图张数
/// @param quality      画质档
/// @param status       任务终态:`done` | `error`
/// @param cost         本次消耗额度(USD 名义计价),可空
/// @param elapsedMs    生成耗时毫秒
/// @param groupName    计费分组名
/// @param keyId        使用的 API Key id,可空
/// @param error        失败原因;成功时为 null
/// @param outputImages 输出图列表,url 已现签
/// @param refImages    参考图列表,url 已现签
public record GenerationDetail(
        String id, Instant createdAt, String prompt, String size, int n, String quality,
        String status, Double cost, int elapsedMs, String groupName, Long keyId, String error,
        List<Image> outputImages, List<Image> refImages) {
}
