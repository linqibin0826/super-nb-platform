/// infra 出站分支按能力分包的根命名空间,自身不放直接类型,四个子包各自实现一类 domain 端口:
///
/// - `persistence/`:写侧 JPA 适配器(聚合仓储)+ Spring Data DAO + JPA 实体
/// - `read/`:读侧适配器(CQRS 读投影)+ Entity → 读视图的手写映射
/// - `storage/`:R2 对象存储适配器(AWS SDK v2,S3 协议)
/// - `thumbnail/`:缩略图生成适配器(JDK ImageIO)
///
/// 子包与 domain/port 下的端口分类一一对应({Entity}Repository → persistence、{Entity}ReadPort → read、
/// {Function}Port → storage/thumbnail),命名与包位置照 tech/port-service.md。
package me.supernb.gallery.infra.adapter;
