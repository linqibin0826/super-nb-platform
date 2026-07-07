package me.supernb.gallery.app.usecase.generation.command;

import dev.linqibin.commons.cqrs.Command;

/// 删除本人一条生成记录(连带清理 R2 对象)。不存在/非本人 → 404。
public record DeleteGenerationCommand(long id, long userId) implements Command<Void> {
}
