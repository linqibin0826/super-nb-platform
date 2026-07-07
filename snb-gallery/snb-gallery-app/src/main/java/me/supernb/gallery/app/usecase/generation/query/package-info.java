/// 生成子域的只读查询用例:`GenerationQueryService`(生成历史列表 + 详情)。
///
/// 查询不经 `CommandBus`——无副作用,由 adapter 直接注入调用(tech/commandbus.md 的 Query 端策略);
/// 本类不定义接口,需要 mock 测试时直接 mock 它调用的端口(tech/port-service.md)。
package me.supernb.gallery.app.usecase.generation.query;
