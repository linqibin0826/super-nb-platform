/// infra 出站分支按能力分包的根命名空间,自身不放直接类型,两个子包各自实现一类 domain 端口:
///
/// - `persistence/`:写侧 JPA 适配器(`CampaignAdapter`、`DrawAdapter`)+ Spring Data DAO + JPA 实体;
///   `DrawPort` 虽属 Driven Port(外部能力/领域动作),但因落库仍归并到这里,不单开 `draw/` 子包
/// - `read/`:读侧适配器(CQRS 读投影 `PoolReadAdapter`)+ 消费 snb-sub2api 防腐层的薄适配(`RechargeReadAdapter`)
///
/// 子包与 domain/port 下的端口分类对应(`{Entity}Repository`/写库的 Driven Port → persistence、
/// `{Entity}ReadPort` → read),命名与包位置照 tech/port-service.md。
package me.supernb.activity.infra.adapter;
