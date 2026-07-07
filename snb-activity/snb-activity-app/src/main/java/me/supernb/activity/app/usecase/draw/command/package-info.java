/// 抽奖子域的命令:`PerformDrawCommand`——当前登录用户在进行中活动里抽一次,返回类型复用 domain 既有的
/// `DrawResult`,不另立写结果 DTO。
///
/// 命令是纯数据载体(record 实现 `Command<R>`),不带业务逻辑;经 CommandBus 派发到同子域包下的
/// `PerformDrawHandler`。
package me.supernb.activity.app.usecase.draw.command;
