/// content 只读投影适配器：JdbcClient 原生 SQL（tag 过滤要 jsonb `@>` 包含运算，HQL 表达不了）。
/// 拼接片段全为常量，入参一律 `:param` 绑定，不留注入面（照 PromptReadAdapter 的纪律）。
package me.supernb.content.infra.adapter.read;
