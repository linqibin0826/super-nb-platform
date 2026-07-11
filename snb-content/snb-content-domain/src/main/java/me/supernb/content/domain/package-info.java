/// content 限界上下文（内容中心 hub.super-nb.me）的 domain 层根命名空间：纯 Java 业务规则与只读契约，
/// 不依赖任何框架（ArchUnit `domainIsFrameworkFreeAndInward` + Gradle `enforceDomainPurity` 双重强制）。
///
/// - `model/read/`：对外读视图 record（原样透出到 REST 响应，不另建 app 层 DTO）
/// - `port/`：端口定义（`repository/` 写、`read/` 只读投影）
/// - `exception/`：业务异常（trait 驱动 HTTP 映射，无手写 @ControllerAdvice）
package me.supernb.content.domain;
