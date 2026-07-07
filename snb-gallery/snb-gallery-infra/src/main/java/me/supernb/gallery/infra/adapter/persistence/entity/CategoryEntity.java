package me.supernb.gallery.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 类目 JPA 实体,映射 `gallery.category`。
///
/// 聚合根,继承 [BaseJpaEntity];类目数据由收录管线以纯 SQL 直接写入维护,
/// 应用侧只读,不提供业务构造器。
@Entity
@Table(name = "category", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity extends BaseJpaEntity {

    /// 类目短标识,全局唯一。
    @Column(name = "slug")
    private String slug;

    /// 所属类目轴:`scene` | `style` | `subject`。
    @Column(name = "axis")
    private String axis;

    /// 类目英文名称。
    @Column(name = "name_en")
    private String nameEn;

    /// 类目中文名称。
    @Column(name = "name_zh")
    private String nameZh;

    /// 列表展示排序权重。
    @Column(name = "sort")
    private Integer sort;
}
