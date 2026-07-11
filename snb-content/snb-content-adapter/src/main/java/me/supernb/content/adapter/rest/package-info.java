/// content REST 入站：唯一 ContentController（/content/v1）。读操作直接调用注入的查询用例、
/// 把 domain/model/read 的读视图原样当响应体；写操作只注入 CommandBus（ArchUnit adapterInjectsBusNotHandlers）。
/// MVP 公开只读无鉴权；admin 端点由 adapter.web.AdminTokenFilter 把门。
package me.supernb.content.adapter.rest;
