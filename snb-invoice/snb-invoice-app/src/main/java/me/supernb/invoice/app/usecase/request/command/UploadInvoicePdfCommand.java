package me.supernb.invoice.app.usecase.request.command;

import dev.linqibin.commons.cqrs.Command;

/// 管理员上传发票 PDF 命令(INVOICING→ISSUED;ISSUED 重传覆盖)。
public record UploadInvoicePdfCommand(long requestId, String filename, byte[] bytes) implements Command<Void> {
}
