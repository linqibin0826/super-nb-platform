package me.supernb.gallery.app.usecase.generation.command;

/// 写侧输出图字节载体(base64 已由 adapter 解码)。
public record ImageBytes(byte[] data) {
}
