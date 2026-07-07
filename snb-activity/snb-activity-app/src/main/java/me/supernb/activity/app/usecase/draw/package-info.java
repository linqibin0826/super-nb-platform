/// 抽奖子域:写路径与查询用例都在这里。
///
/// - (本包)`PerformDrawHandler`:CommandHandler 实现,处理一次抽奖动作
/// - `command/`:`PerformDrawCommand`——抽奖命令
/// - `query/`:三个查询用例——抽奖资格状态、我的中奖历史、近期真实中奖信息流
///
/// Handler 直接住子域包(不另起子目录),命令单独落 `command/`;并发正确性(同一用户不超额领奖)由 domain 的
/// `DrawPort` 实现(infra 的 advisory lock + `FOR UPDATE SKIP LOCKED` + 事务)保证,Handler 本身无事务、纯编排。
package me.supernb.activity.app.usecase.draw;
