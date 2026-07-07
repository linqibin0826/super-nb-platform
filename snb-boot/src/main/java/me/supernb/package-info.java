/// snb-platform 单体应用的组装点(composition root)。直接持有唯一 `@SpringBootApplication`
/// 入口 [SnbPlatformApplication];唯一的子包 `config/` 放横切 `@Configuration`
/// (目前只有 JPA 审计操作人来源 `CurrentUserAuditorConfig`)。
///
/// 全仓库唯一同时依赖各上下文 adapter + infra + api 三层的模块——adapter 与 infra
/// 编译期互不相连(各自只经 app 传递依赖 domain),靠这里把 domain 定义的端口接口
/// 在运行期接起来。本包不写业务代码;守门测试(ArchUnit 依赖边界门禁、各上下文
/// WiringTest)住旁边 `src/test` 的 `me.supernb.boot` 包,不在这里。
package me.supernb;
