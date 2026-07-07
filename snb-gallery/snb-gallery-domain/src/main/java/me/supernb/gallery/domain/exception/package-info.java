/// gallery 业务异常存放处:全部继承 commons 的 DomainException,构造时带 StandardErrorTrait
/// 语义特征,由 commons 统一错误处理自动映射成 RFC 9457 problem+json 响应。
///
/// 当前只有一种失败形状(资源找不到),收在单个 [GalleryException] 里按场景开静态工厂,
/// 没有必要拆成多个异常类。
package me.supernb.gallery.domain.exception;
