/// 生成子域的写命令:`CreateGenerationCommand`(创建)、`DeleteGenerationCommand`(删除),
/// 及命令内嵌的图片字节载体 `ImageBytes`/`RefBytes`(base64 已由 adapter 解码,不独立作端口契约)。
///
/// 命令是纯数据载体(见 tech/commandbus.md):不放业务逻辑,字段校验最多到紧凑构造器非空;
/// 经 `CommandBus.handle()` 按类型路由到同子域包下的 `{Action}{Entity}Handler`。
package me.supernb.gallery.app.usecase.generation.command;
