/// content 上下文对外契约（当前空壳预留）。
///
/// 第一笔跨上下文调用出现时：本上下文对外的 DTO / 接口定义进这里，
/// 消费方 infra 依赖本模块做薄适配——上下文之间不得直接依赖彼此的 domain/app（ArchUnit 门禁）。
package me.supernb.content.api;
