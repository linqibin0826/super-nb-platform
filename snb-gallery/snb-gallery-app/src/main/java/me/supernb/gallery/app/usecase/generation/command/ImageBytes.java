package me.supernb.gallery.app.usecase.generation.command;

/// 写侧输出图字节载体:一张待上传的输出图,base64 已由 adapter 解码为原始字节(命令里以 List 承载多张)。
///
/// @param data 图片原始字节
public record ImageBytes(byte[] data) {
}
