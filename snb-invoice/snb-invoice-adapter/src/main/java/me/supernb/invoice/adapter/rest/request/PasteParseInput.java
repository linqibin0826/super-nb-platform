package me.supernb.invoice.adapter.rest.request;

/// 开票资料 AI 识别入参:用户粘贴的整段文本。
public record PasteParseInput(String text) {
}
