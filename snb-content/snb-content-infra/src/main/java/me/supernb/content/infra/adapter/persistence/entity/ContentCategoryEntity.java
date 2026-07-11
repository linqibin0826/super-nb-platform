package me.supernb.content.infra.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 分类 JPA 实体，映射 `content.category`。配置型查找表（slug 即主键），不上审计基座。
@Entity
@Table(name = "category", schema = "content")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentCategoryEntity {

    /// 分类键（categories.yml 的 slug）。
    @Id
    private String slug;

    @Column(nullable = false)
    private String name;

    /// 列表页 tab 排序。
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /// 新建一条分类。
    public ContentCategoryEntity(String slug, String name, int sortOrder) {
        this.slug = slug;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    /// 整表同步时更新名称与排序（slug 不变）。
    public void rename(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
