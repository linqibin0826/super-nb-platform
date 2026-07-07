/// 用例编排的根命名空间,按业务子域分包,本包自身不放任何类型。
///
/// - `draw/`:抽奖子域——写路径(`PerformDrawCommand` + `PerformDrawHandler`)+ 查询用例(资格状态/我的中奖/近期中奖)
/// - `campaign/`:活动期子域——目前只有查询用例(充值榜/奖池实况/近期充值动态),没有写路径
///
/// 子域划分照 patra-catalog 的做法,按业务动作分组,不按技术分层;每个子域包再按 `command/`、`dto/`、`query/`
/// 拆分写命令、写结果与查询用例(拆分细则见各子域包的说明)。
package me.supernb.activity.app.usecase;
