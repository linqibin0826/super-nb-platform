package me.supernb.gallery.adapter.rest.request;

import java.util.List;

/// 创建生成记录请求(图以 base64 传输,adapter 解码后组命令)。
public record CreateGenerationRequest(
        String id, String prompt, String size, int n, String quality, String status,
        Double cost, int elapsedMs, String groupName, Long keyId, String error,
        List<ImagePayload> outputImages, List<RefPayload> refImages) {

    /// 一张输出图的 base64 载体。
    public record ImagePayload(String b64) {
    }

    /// 一张参考图的 base64 载体(带内容类型)。
    public record RefPayload(String b64, String contentType) {
    }
}
