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
/// 聚合根,继承 [BaseJpaEntity];类目由收录管线纯 SQL 维护,应用侧只读,无业务构造器。
@Entity
@Table(name = "category", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity extends BaseJpaEntity {

    /// 类目短标识(全局唯一)。
    @Column(name = "slug")
    private String slug;

    /// 类目轴:`scene` | `style` | `subject`。
    @Column(name = "axis")
    private String axis;

    /// 英文名。
    @Column(name = "name_en")
    private String nameEn;

    /// 中文名。
    @Column(name = "name_zh")
    private String nameZh;

    /// 展示排序权重。
    @Column(name = "sort")
    private Integer sort;
}
