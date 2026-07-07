/// 读侧视图 record,一文件一类,不属于聚合、无业务逻辑,只承载查询结果的数据形状
/// (patra Read Model 同款)。
///
/// `DrawStatus`、`LeaderEntry`、`MyDrawView`、`PoolTier`、`PublicDraw`、`RechargeEntry`
/// 原样透出到 REST 响应,domain 与 API 契约在这里合流,不再另起一套 app 层 DTO 做二次
/// 映射;`RawDraw`、`RawWinner`、`CodeStatus` 是查询用例内部流转的未加工/中间数据,不
/// 直接对外。
package me.supernb.activity.domain.model.read;
