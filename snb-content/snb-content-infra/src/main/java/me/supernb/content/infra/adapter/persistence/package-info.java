/// content 聚合仓储适配器：JPA 实体（entity/）+ Spring Data DAO（dao/）+ 端口实现。
/// 事务边界用 TransactionTemplate 显式管理（不用 @Transactional，避免自调用代理坑）。
package me.supernb.content.infra.adapter.persistence;
