/// 全部端口(纯接口)的命名空间,按类型分子包;端口在 domain 定义,由 infra 实现,
/// domain/app 只依赖接口本身。
///
/// - `campaign/`:`CampaignPort`——活动期查询端口
/// - `draw/`:`DrawPort`——抽奖原子事务,外部能力/领域动作类端口
/// - `read/`:`PoolReadPort`、`RechargeReadPort`——CQRS 读投影端口
///
/// 端口形状由用例需求决定,不是持久化表结构的直接投影(说"资格金额",不说原始订单行);
/// 命名与包位置细则见 tech/port-service.md。本包自身不含直接类型。
package me.supernb.activity.domain.port;
