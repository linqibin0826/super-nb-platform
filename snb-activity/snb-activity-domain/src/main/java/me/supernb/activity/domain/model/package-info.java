/// 业务规则与不变量的纯计算:`Campaign`(活动期)、`DrawEligibility`(抽奖资格规则)、
/// `DrawResult`(单次抽奖结果)。
///
/// 均为不可变 record / 纯函数工具类,不是 DDD 教科书意义上的"实体"——没有聚合根基类、
/// 没有版本锁、没有领域事件那套仪式。读侧视图放在子包 `read/`,与本包的写侧不变量物理
/// 隔离、语义不同。
package me.supernb.activity.domain.model;
