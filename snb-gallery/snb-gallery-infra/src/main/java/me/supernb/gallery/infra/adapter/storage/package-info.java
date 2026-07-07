/// gallery 私有对象存储适配:实现 domain/port/storage 的 `ImageStoragePort`,AWS SDK v2 对接
/// R2(S3 协议兼容)。
///
/// `GalleryR2Properties`(`gallery.r2.*`,凭据仅经环境变量注入,绝不入库)驱动 `R2Config` 装配
/// `S3Client`(写删用)与 `S3Presigner`(限时签 URL 用,优先取面向浏览器的 public endpoint,
/// 未配回落主 endpoint)。真正的端口实现 `R2StorageAdapter` 不是 `@Component`,由 `R2Config`
/// 的 `@Bean` 方法手工 new 出来注入,不参与组件扫描。
///
/// `R2Config` 只在 `gallery.r2.endpoint` 配置存在时激活(`@ConditionalOnProperty`),缺配环境
/// (部分测试)不装配这层,改由测试自行提供 mock `ImageStoragePort`;条件装配与凭据纪律见 infra.md。
package me.supernb.gallery.infra.adapter.storage;
