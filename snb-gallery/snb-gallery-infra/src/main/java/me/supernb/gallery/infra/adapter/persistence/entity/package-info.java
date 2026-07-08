/// gallery 上下文的 JPA 实体所在包,映射 `gallery` schema 下的全部表;一表一类,
/// 类名统一为 `{Entity}Entity`。
///
/// 三档 patra 审计基座按聚合语义选:聚合根用 `BaseJpaEntity`——category、prompt、
/// generation;有独立生命周期/独立更新时点的子实体用 `ChildJpaEntity`——prompt_like、
/// prompt_favorite、ref_image;完全随聚合根级联生死的值对象用 `ValueObjectJpaEntity`——
/// generation_image、generation_ref。
///
/// 六边形边界上的纯技术细节层:实体类型不出这一层——`persistence/dao/` 下的
/// Spring Data 仓储、`persistence/` 下的写侧适配器,以及 `read/` 包的
/// `PromptReadAdapter`/`PromptMapper` 都直接依赖实体类型;经 `read/` 的手写 mapper
/// 转换成 domain/model/read 的读视图后,实体类型才不再继续往上传递;
/// domain/app 全程不感知 JPA 的存在。
package me.supernb.gallery.infra.adapter.persistence.entity;
