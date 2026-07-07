/// sub2api 防腐层 starter:面向上游 sub2api 系统的共享能力,自动配置自注册(不依赖宿主组件扫描)。
///
/// - `auth/`:introspect 鉴权客户端 + @CurrentUser 参数解析器(所有上下文共用,web 环境自动生效)
/// - `recharge/`:充值只读读模型(配 `sub2api.read-datasource.url` 才装配)
/// - `autoconfig/`:@AutoConfiguration 装配与配置属性
///
/// 消费纪律:各限界上下文在自己的 app 层定义端口、infra 层薄适配到这里;
/// 本包类型只允许出现在 infra / adapter 层(ArchUnit 门禁)。
package me.supernb.sub2api;
