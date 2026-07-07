/// 生成子域:创建/删除一条 AI 生成历史记录,图片经 R2 对象存储、缩略图与参考图内容去重。
/// 写处理器 `CreateGenerationHandler`、`DeleteGenerationHandler` 直接住本包(照 patra-catalog,
/// Handler 与子域包同级,不再下沉一层)。
///
/// - `command/`:写命令(`CreateGenerationCommand`、`DeleteGenerationCommand`)与命令自有字节载体
/// - `dto/`:写结果(`Created`)
/// - `query/`:只读查询用例(`GenerationQueryService`)
///
/// 六边形分层里这是 app 层的一块用例编排:写经 `CommandBus` 路由到这里的 Handler,
/// 读由 adapter 直接注入 `GenerationQueryService`;两条路径都不感知 infra 的持久化技术。
package me.supernb.gallery.app.usecase.generation;
