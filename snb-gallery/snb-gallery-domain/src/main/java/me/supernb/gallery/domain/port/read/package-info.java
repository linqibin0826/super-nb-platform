/// CQRS 读投影端口(`{Entity}ReadPort`),对应跨聚合的列表/统计/跨库读,由 infra 的
/// `{Entity}ReadAdapter` 实现。当前只有 [PromptReadPort] 一个类型:提示词的列表/详情/类目树查询。
package me.supernb.gallery.domain.port.read;
