/// 互动子域的命令:点赞/收藏 toggle 各一个 record,实现 commons-core 的 `Command` 标记接口
/// (泛型参数为该命令的返回类型:`TogglePromptLikeCommand` 绑 `LikeResult`、
/// `TogglePromptFavoriteCommand` 绑 `FavResult`)。
///
/// 命名统一 `{Action}{Entity}Command`;字段全部是原始类型,纯数据载体不带业务逻辑,
/// toggle 幂等与并发不变量在 Handler 与 `InteractionRepository` 实现层兜底,不在这里校验。
package me.supernb.gallery.app.usecase.interaction.command;
