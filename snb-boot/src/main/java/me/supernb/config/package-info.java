/// boot 层横切 `@Configuration` 装配,当前只有 JPA 审计操作人来源
/// [CurrentUserAuditorConfig]。
///
/// 是 snb-sub2api 鉴权结果与 JPA 审计基座之间唯一的交汇点,只能住在两者都可见的
/// 组装点(composition root),不属于任何一个限界上下文私有。
package me.supernb.config;
