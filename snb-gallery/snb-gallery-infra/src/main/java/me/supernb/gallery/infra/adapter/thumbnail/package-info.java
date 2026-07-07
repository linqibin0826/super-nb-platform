/// 缩略图生成适配:实现 domain/port/thumbnail 的 `ThumbnailPort`。
///
/// 唯一类型 `ImageIoThumbnailAdapter`,标 `@Component`——无条件装配,不像 storage 包的
/// `R2StorageAdapter` 要看配置开关才组装。用 JDK 内置 ImageIO 缩放并统一编码为 PNG,
/// 不依赖任何 webp 等第三方编解码库。
package me.supernb.gallery.infra.adapter.thumbnail;
