/// 充值 / 兑换码只读读模型:面向 sub2api 库的读投影契约,配 `sub2api.read-datasource.url` 才装配。
///
/// [RechargeReadModel] 是端口形状,[JdbcRechargeReadModel] 是唯一实现(JdbcTemplate + 独立只读 DataSource,
/// SQL 显式收敛在单文件)。邮箱脱敏在实现内部完成,未脱敏的完整邮箱不跨出本包;
/// 上下文侧消费走薄适配(样板见 activity 的 RechargeReadAdapter),domain/app 不感知这里的存在。
package me.supernb.sub2api.recharge;
