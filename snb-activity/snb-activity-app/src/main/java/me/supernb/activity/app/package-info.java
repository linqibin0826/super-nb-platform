/// activity 上下文的用例编排层:校验请求 → 调用 domain 定义的端口 → 组装读视图/写结果,业务规则本身留在 domain。
///
/// - `usecase/`:按业务子域(`draw`、`campaign`)分包的写处理器与查询用例,见该包说明
///
/// 六边形架构里的 app 层:写路径由 adapter 经 CommandBus 派发到这里的 Handler,读路径由 adapter 直接注入这里的
/// QueryService;向下只依赖 domain 定义的端口接口,不感知 infra/adapter 的存在,也不 import 任何持久化框架类型
/// (jakarta.persistence / hibernate / spring-data 一律禁止)。事务边界不放在这层——收在 infra 端口实现内用
/// TransactionTemplate 显式管理(与 patra 的差异)。
package me.supernb.activity.app;
