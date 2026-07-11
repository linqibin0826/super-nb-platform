/// content 上下文的入站适配层：REST 协议转换，不写业务判断。
///
/// - `rest/`：`ContentController`（`/content/v1`）及其 request record
/// - `web/`：上下文级横切（admin token 门 + 按 IP 令牌桶限流，均只作用于 `/content/v1/` 前缀）
package me.supernb.content.adapter;
