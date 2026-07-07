package me.supernb.gallery.domain.model.read;

/// 一张图([GenerationDetail] 的输出图/参考图元素),url 为已现签的限时下载地址。
///
/// @param url    presigned GET URL(限时,详情查询时现签)
/// @param width  像素宽,可空——当前生成历史两处现签调用都未回填,恒为 null
/// @param height 像素高,可空——语义同 width
public record Image(String url, Integer width, Integer height) {
}
