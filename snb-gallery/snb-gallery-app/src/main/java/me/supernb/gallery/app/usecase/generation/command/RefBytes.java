package me.supernb.gallery.app.usecase.generation.command;

/// 写侧参考图字节载体(base64 已由 adapter 解码)。
public record RefBytes(byte[] data, String contentType) {
}
