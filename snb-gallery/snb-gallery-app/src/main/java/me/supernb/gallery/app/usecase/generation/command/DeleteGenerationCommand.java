package me.supernb.gallery.app.usecase.generation.command;

import dev.linqibin.commons.cqrs.Command;

/// 删除本人一条生成记录的写命令(级联清理 R2 对象,细节见 `DeleteGenerationHandler`)。
/// 不存在或不归属本人 → 404。返回 `Void`——Handler 里 `return null`(标准 Void 命令写法,见 tech/commandbus.md)。
///
/// @param id     生成记录 id(路径参数照常收 long;非法数字在 Spring 绑定阶段即 400,不到这层)
/// @param userId 当前登录用户 id,用于校验记录归属
public record DeleteGenerationCommand(long id, long userId) implements Command<Void> {
}
