/// 跨上下文共享的纯 web 横切,不属于任何一个限界上下文:目前只有 [UnauthorizedException]
/// (平台统一 401,`@CurrentUser` 解析失败即抛出)。
///
/// 与 snb-sub2api 并列的两个共享模块之一;activity/gallery 各自的 adapter 按需挂它,
/// domain/app 不感知——鉴权是跨上下文能力,不归任何一个上下文私有。
package me.supernb.common;
