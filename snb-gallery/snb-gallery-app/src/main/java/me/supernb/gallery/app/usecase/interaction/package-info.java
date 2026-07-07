/// gallery 互动子域(点赞/收藏 toggle)的用例层:写处理器直接住本包,
/// 命令、写结果、只读查询各自收在子包里。
///
/// - `command/`:命令 record(`TogglePromptLikeCommand`、`TogglePromptFavoriteCommand`)
/// - `dto/`:写结果 record(`LikeResult`、`FavResult`)
/// - `query/`:只读查询用例(`InteractionQueryService`)
///
/// 六边形 app 层:写经 CommandBus 派发到本包内的 `TogglePromptLikeHandler`/
/// `TogglePromptFavoriteHandler`,读由 adapter 直接注入 `query/` 下的查询服务
/// (规范见 tech/commandbus.md);本包不感知持久化技术,toggle 的并发/幂等不变量
/// 由 domain/port 的 `InteractionRepository` 实现兜底,这里只做编排。
package me.supernb.gallery.app.usecase.interaction;
