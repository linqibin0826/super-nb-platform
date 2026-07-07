package me.supernb.gallery.domain.port.thumbnail;

/// 缩略图生成端口。
public interface ThumbnailPort {

    /// 原图字节 → 长边 ≤ maxEdge 的 PNG 字节(保比例、只缩不放)。坏图抛异常(调用方尽力而为)。
    byte[] toPng(byte[] src, int maxEdge);
}
