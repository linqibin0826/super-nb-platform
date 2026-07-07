/// 互动子域的写结果:点赞/收藏 toggle 各一个 record(`LikeResult`、`FavResult`),
/// 是对应 Handler 的返回值,经 CommandBus 原样透出给 adapter 当 REST 响应体。
///
/// 形状统一是"计数 + 当前态"二元组(如 `likeCount`/`liked`),跟 domain/model/read
/// 下面向列表/详情的读视图 record 是两回事——只服务写路径的即时反馈,不是查询契约。
package me.supernb.gallery.app.usecase.interaction.dto;
