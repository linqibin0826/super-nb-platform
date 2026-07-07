/// domain 端口(接口)的根命名空间,按类型分四个子包,实现全部在 infra:
///
/// - `repository/`:聚合持久化端口(`{Entity}Repository`)
/// - `read/`:CQRS 读投影端口(`{Entity}ReadPort`)
/// - `storage/`:对象存储领域动作端口(`{Function}Port`)
/// - `thumbnail/`:缩略图生成领域动作端口(`{Function}Port`)
///
/// 端口形状由用例需求决定,不是底层存储表结构的直接投影;四类端口的命名与包位置按性质
/// 固定对应,不能混着来。
package me.supernb.gallery.domain.port;
