/// activity 上下文的领域层:业务规则与不变量的纯计算、读侧视图、端口定义、业务异常。
///
/// - `model/`:不变量的纯计算(`Campaign`、`DrawResult`、`DrawEligibility`)
/// - `model/read/`:读侧视图 record,一文件一类,原样透出到 REST 响应
/// - `port/`:全部端口(纯接口),按类型分子包(`campaign/`、`draw/`、`read/`)
/// - `exception/`:业务异常
///
/// 六边形架构的核心层:零框架依赖(禁 Spring / JPA / Hibernate,ArchUnit 门禁与 Gradle
/// 的 `enforceDomainPurity` 任务双重强制),不依赖 app/infra/adapter,也不依赖 gallery
/// 上下文。本仓刻意让 domain 薄——聚合生命周期简单,不引入聚合根基类/版本锁/领域事件那套
/// 仪式,只留业务规则与不变量的纯计算(与 patra 的刻意差异)。
package me.supernb.activity.domain;
