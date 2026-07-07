/// gallery 提示词(prompt)子域的用例层命名空间;当前只有 `query/`,没有 `command/`、`dto/`。
///
/// - `query/`:提示词只读查询用例(`PromptQueryService`)
///
/// prompt 没有业务写路径——提示词由运维 SQL / 收录管线维护,不经应用层命令创建或修改
/// (实体不写业务构造器,见 tech/jpa.md 的基座选型);与 `interaction/` 是同级的另一个子域包,
/// 哪天 prompt 长出写路径,再按 `usecase/{子域}/{command,dto}/` 的既有约定补齐。
package me.supernb.gallery.app.usecase.prompt;
