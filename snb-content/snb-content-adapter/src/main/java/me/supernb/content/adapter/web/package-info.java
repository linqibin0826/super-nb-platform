/// content 上下文级 web 横切：`AdminTokenFilter`（admin 前缀 token 门，fail-closed）+
/// `RateLimitFilter`（按 IP 令牌桶，状态载体 `TokenBucket` 逐字节拷自 gallery——adapter 模块间
/// 禁互相依赖是 ArchUnit 门禁，拷贝是既定模式）。两者都只作用于 `/content/v1/` 路径前缀，
/// 绝不把逻辑套到其他上下文或付费模型 API 路径上。
package me.supernb.content.adapter.web;
