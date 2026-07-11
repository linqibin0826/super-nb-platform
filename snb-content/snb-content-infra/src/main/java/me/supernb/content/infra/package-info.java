/// content 上下文的 infra 层：实现 domain 端口的技术细节（JPA 持久化 + JdbcClient 只读投影）。
///
/// - `adapter/persistence/`：聚合仓储适配器（含 `entity/`、`dao/`）
/// - `adapter/read/`：只读投影适配器（原生 SQL，含 jsonb tag 过滤）
package me.supernb.content.infra;
