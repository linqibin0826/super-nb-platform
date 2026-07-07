/// gallery 限界上下文的 domain 层根命名空间:纯 Java 业务规则与只读契约,不依赖任何框架
/// (ArchUnit `domainIsFrameworkFreeAndInward` + Gradle `enforceDomainPurity` 双重强制,
/// 连 Spring 注解都不许出现)。
///
/// - `model/`:业务规则计算(及其 `read/`、`enums/` 两个子包,分别放读视图与枚举)
/// - `port/`:全部端口定义(`repository/`、`read/`、`storage/`、`thumbnail/`)
/// - `exception/`:业务异常
///
/// 本上下文刻意保持薄:聚合生命周期简单,不跟进聚合根基类、版本锁、领域事件那套仪式。
package me.supernb.gallery.domain;
