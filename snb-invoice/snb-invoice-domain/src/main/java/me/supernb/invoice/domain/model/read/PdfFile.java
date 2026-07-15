package me.supernb.invoice.domain.model.read;

/// 发票 PDF 文件(下载投影)。
public record PdfFile(String filename, byte[] bytes) {
}
