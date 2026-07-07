/// gallery 上下文的出站分支根包,只含一个子包 `adapter/`,住全部 domain 端口的技术实现
/// (JPA 持久化、只读投影、R2 对象存储、缩略图生成)。
///
/// 六边形边界上的出站分支:实现 domain/port 定义的接口,技术细节(JPA/Hibernate/AWS SDK)
/// 全收在这一层,domain/app 不感知;与入站的 adapter 模块在 Gradle 依赖图上是两条互不相连的
/// 分支,只在 snb-boot 组装时由 Spring 按接口类型注入接起来。
package me.supernb.gallery.infra;
