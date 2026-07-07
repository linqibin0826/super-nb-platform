/// gallery 上下文的 REST Controller 所在包:单一入口 `GalleryController`,路径 `/gallery/v1/*`——
/// 一个上下文一个 Controller,子域划分(prompt/interaction/generation)体现在注入的查询用例与
/// 派发的命令上,不体现在 Controller 数量上。
///
/// - `request/`:请求 DTO(record,一文件一类,当前只有 `CreateGenerationRequest`)
/// - `response/`:响应 DTO(record,一文件一类,当前只有 `DeleteResponse`)
///
/// Controller 只做协议转换:写操作把请求体/路径参数组装成命令经 `CommandBus` 派发,
/// 读操作直接调用注入的查询用例、把 domain/model/read 的读视图原样当响应体返回,不另建 DTO;
/// 鉴权靠方法参数 `@CurrentUser UserProfile`,解析逻辑收在 snb-sub2api starter,本包不重复实现。
package me.supernb.gallery.adapter.rest;
