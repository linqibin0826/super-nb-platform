/// gallery 上下文的请求 DTO:record,一文件一类,专给 Controller 反序列化 JSON 请求体。
///
/// 当前只有 `CreateGenerationRequest`,对应写端点 `POST /me/generations`——其余写端点
/// (点赞/取消点赞/收藏/取消收藏/删除生成记录)入参只有路径变量与登录态,不需要请求体,不进这个包。
package me.supernb.gallery.adapter.rest.request;
