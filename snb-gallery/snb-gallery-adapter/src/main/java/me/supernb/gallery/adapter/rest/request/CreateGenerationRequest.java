package me.supernb.gallery.adapter.rest.request;

import java.util.List;

/// 创建生成记录请求(图以 base64 传输,adapter 解码后组装成写命令)。
///
/// 字段基本对应写命令,只少一个 userId——生成记录的归属由 `@CurrentUser` 另行解析注入,
/// 不信任请求体自带的用户身份。
///
/// @param prompt       生成提示词
/// @param size         尺寸档(如 `1024x1024`)
/// @param n            出图张数
/// @param quality      画质档
/// @param status       任务终态:`done` | `error`
/// @param cost         本次消耗额度(USD 名义计价)
/// @param elapsedMs    生成耗时毫秒
/// @param groupName    计费分组名
/// @param keyId        使用的 API Key id
/// @param error        失败原因(成功为 null)
/// @param outputImages 输出图列表,每张携带未解码的 base64
/// @param refImages    参考图列表,每张携带未解码的 base64 与内容类型
public record CreateGenerationRequest(
        String prompt, String size, int n, String quality, String status,
        Double cost, int elapsedMs, String groupName, Long keyId, String error,
        List<ImagePayload> outputImages, List<RefPayload> refImages) {

    /// 一张输出图的 base64 载体,adapter 解码后转成 `ImageBytes`。
    ///
    /// @param b64 图片内容的 base64 编码
    public record ImagePayload(String b64) {
    }

    /// 一张参考图的 base64 载体(带内容类型),adapter 解码后转成 `RefBytes`。
    ///
    /// @param b64         图片内容的 base64 编码
    /// @param contentType MIME 类型(如 `image/png`);为 null 时下游按 png 处理
    public record RefPayload(String b64, String contentType) {
    }
}
