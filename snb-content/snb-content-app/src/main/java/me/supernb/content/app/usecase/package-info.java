/// content 用例层：按子域分 article/、category/；写用例 = CommandHandler（CommandBus 派发），
/// 读用例 = 查询服务（Controller 直接注入，直返 domain 读视图）。
package me.supernb.content.app.usecase;
