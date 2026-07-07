/// `PoolReadPort`(奖池档位余量统计)、`RechargeReadPort`(充值/兑换码只读投影,委托
/// snb-sub2api 的 RechargeReadModel)——CQRS 读投影端口,domain/port/read 分类,命名
/// 一律 `{Entity}ReadPort`。只出查询用例需要的字段,敏感列(redeem_code、claimed_by 等)
/// 绝不透出。
package me.supernb.activity.domain.port.read;
