/// 按业务子域分包的用例集合,子域划分照业务动作分组,不是技术分层。
///
/// - `prompt/`:提示词库查询(公开只读——prompt 由运维 SQL/收录管线维护,应用侧无业务写路径)
/// - `interaction/`:点赞/收藏开关命令与我的互动查询
/// - `generation/`:创建/删除生成记录命令与生成历史查询
///
/// 每个子域包内部再按 `{Action}{Entity}Handler`(住子域包本身)/ `command/` / `dto/` / `query/`
/// 细分,命名与位置规范见 app 层开发规范(`.claude/rules/layers/app.md`)。
package me.supernb.gallery.app.usecase;
