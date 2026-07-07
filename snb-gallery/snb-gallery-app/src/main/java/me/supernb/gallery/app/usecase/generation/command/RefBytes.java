package me.supernb.gallery.app.usecase.generation.command;

/// 写侧参考图字节载体:一张待上传的参考图,base64 已由 adapter 解码为原始字节(命令里以 List 承载多张)。
///
/// @param data        图片原始字节
/// @param contentType MIME 类型(如 `image/png`/`image/jpeg`);为 null 时按 png 处理(见 `CreateGenerationHandler`)
public record RefBytes(byte[] data, String contentType) {
}
