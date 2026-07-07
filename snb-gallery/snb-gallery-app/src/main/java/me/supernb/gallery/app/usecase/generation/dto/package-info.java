/// 生成子域的写结果:`Created`(创建成功后的对外可见结果)。
///
/// 是 `CreateGenerationCommand` 的返回类型 `R`,只在写路径上产生;不要跟
/// `domain/model/read` 的读视图混淆——那是查询路径的返回形状,两者物理分包、互不复用。
package me.supernb.gallery.app.usecase.generation.dto;
