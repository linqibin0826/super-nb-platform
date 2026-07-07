/// activity 上下文的出站适配层(六边形 infra):实现 domain/port 定义的全部端口,技术细节
/// (JPA 持久化、PG 特有并发原语、sub2api 只读薄适配)全部收敛在这一层,domain/app 不感知。
///
/// - `adapter/`:端口实现按能力分包,`persistence/` 写侧、`read/` 读侧
///
/// 依赖方向指向 domain(经 app 传递);与入站的 adapter 模块编译期互不依赖,两者只在
/// `snb-boot` 由 Spring 按 domain 定义的端口接口做运行期 DI 接起来。
package me.supernb.activity.infra;
