/// @AutoConfiguration 装配入口与配置属性([Sub2apiProperties],前缀 `sub2api.*`);只放装配声明,
/// 能力实现分别住 auth/、recharge/。
///
/// 三块能力独立条件装配:[Sub2apiAutoConfiguration] 常开;[Sub2apiWebAutoConfiguration] 需 servlet web 环境
/// 且 webmvc 在 classpath;[Sub2apiRechargeAutoConfiguration] 需配 `sub2api.read-datasource.url`,
/// 不配就完全不建只读 DataSource。全部经 `META-INF/spring/...AutoConfiguration.imports` 自注册,
/// 不吃宿主(snb-boot)的组件扫描。
package me.supernb.sub2api.autoconfig;
