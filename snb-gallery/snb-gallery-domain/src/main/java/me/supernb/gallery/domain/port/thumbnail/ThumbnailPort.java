package me.supernb.gallery.domain.port.thumbnail;

/// 缩略图生成端口。
public interface ThumbnailPort {

    /// 原图字节 → 长边 ≤ maxEdge 的 PNG 字节,保持宽高比、只缩不放;
    /// 解不出图时抛运行期异常,调用方按尽力而为处理(捕获后放弃缩略图,不阻断主流程)。
    byte[] toPng(byte[] src, int maxEdge);
}
